(ns jiksnu.modules.atom.actions.feed-source-actions
  (:require [aleph.http :as http]
            [ciste.config :refer [config]]
            [ciste.initializer :refer [definitializer]]
            [ciste.core :refer [defaction]]
            [clj-http.client :as client]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.tools.logging :as log]
            [jiksnu.modules.atom.util :as abdera]
            [jiksnu.actions.activity-actions :as actions.activity]
            [jiksnu.actions.feed-source-actions :as actions.feed-source]
            [jiksnu.actions.resource-actions :as actions.resource]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.channels :as ch]
            [jiksnu.model :as model]
            [jiksnu.model.feed-source :as model.feed-source]
            [jiksnu.model.resource :as model.resource]
            [jiksnu.namespace :as ns]
            [jiksnu.ops :as ops]
            [jiksnu.session :as session]
            [jiksnu.templates.actions :as templates.actions]
            [jiksnu.transforms :as transforms]
            [jiksnu.transforms.feed-source-transforms :as transforms.feed-source]
            [jiksnu.util :as util]
            [lamina.core :as l]
            [lamina.time :as lt]
            [lamina.trace :as trace]
            [slingshot.slingshot :refer [throw+ try+]])
  (:import jiksnu.model.FeedSource
           jiksnu.model.User
           org.apache.abdera.model.Feed))

(defn process-entry
  "Create an activity from an atom entry"
  [[feed source entry]]
  (let [params (actions.activity/entry->activity entry feed source)]
    (actions.activity/find-or-create params)))

(defn process-feed
  [^FeedSource source ^Feed feed]
  {:pre [(instance? FeedSource source)
         (instance? Feed feed)]}
  (trace/trace "feeds:processed" feed)

  (when-let [author (abdera/get-feed-author feed)]
    (let [author-id (abdera/get-simple-extension author ns/atom "id")
          params (actions.user/parse-person author)
          params (assoc params :id (:url params))
          user (actions.user/find-or-create params)
          id (:_id user)]
      (model.feed-source/set-field! source :author id)))

  (let [feed-title (.getTitle feed)]
    (when-not (= feed-title (:title source))
      (model.feed-source/set-field! source :title feed-title)))

  (if-let [hub-link (abdera/get-hub-link feed)]
    (model.feed-source/set-field! source :hub hub-link))

  (if (actions.feed-source/watched? source)
    (doseq [entry (abdera/get-entries feed)]
      (l/enqueue ch/pending-entries [feed source entry]))
    (do (log/warnf "no watchers for %s" (:topic source))
        (actions.feed-source/unsubscribe source))))

(defn update*
  [source & [options]]
  {:pre [(instance? FeedSource source)]}
  (if-not (:local source)
    (if-let [topic (:topic source)]
      (if-let [response @(ops/update-resource topic options)]
        (if-let [feed (abdera/parse-xml-string (:body response))]
          (let [feed-updated (coerce/to-date-time (abdera/get-feed-updated feed))
                source-updated (:updated source)]
            (if (or (:force options)
                    (not (and feed-updated source-updated))
                    (time/after? feed-updated source-updated))
              (try+
                (process-feed source feed)
                (catch Throwable ex
                  (trace/trace :errors:handled ex)))
              (log/warn "feed is up to date")))
          (throw+ "could not obtain feed"))
        (log/warn "Could not get resource")))
    (log/warn "local sources do not need updates")))

