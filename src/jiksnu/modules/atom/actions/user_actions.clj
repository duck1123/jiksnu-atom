(ns jiksnu.modules.atom.actions.user-actions
  (:require [clojure.tools.logging :as log]
            [lamina.trace :as trace]
            [jiksnu.model :as model]
            [jiksnu.model.domain :as model.domain]
            [jiksnu.namespace :as ns]
            [jiksnu.ops :as ops]
            [jiksnu.util :as util]
            [slingshot.slingshot :refer [throw+]])
  (:import org.apache.abdera.model.Person))

;; TODO: This function should be called at most once per user, per feed
(defn person->user
  "Extract user information from atom element"
  [^Person person]
  (log/info "converting person to user")
  (trace/trace :person:parsed person)
  (let [{:keys [id username url links note email local-id]
         :as params} (parse-person person)
         domain-name (util/get-domain-name (or id url))
         domain @(ops/get-discovered @(ops/get-domain domain-name))
         username (or username (get-username {:_id id}))]
    (if (and username domain)
      (let [user-meta (model.domain/get-xrd-url domain url)
            user (merge params
                        {:domain domain-name
                         :_id (or id url)
                         :user-meta-link user-meta
                         :username username})]
        (model/map->User user))
      (throw+ "could not determine user"))))

(defn parse-person
  [^Person person]
  {:_id (abdera/get-simple-extension person ns/atom "id")
   :email (.getEmail person)
   :url (str (.getUri person))
   :name (abdera/get-name person)
   :note (abdera/get-note person)
   :username (abdera/get-username person)
   :local-id (-> person
                 (abdera/get-extension-elements ns/statusnet "profile_info")
                 (->> (map #(abdera/attr-val % "local_id")))
                 first)
   :links (abdera/get-links person)})

(defn fetch-user-feed
  "returns a feed"
  [^User user & [options]]
  (if-let [url (model.user/feed-link-uri user)]
    (let [response (ops/update-resource url)]
      #_(abdera/parse-xml-string (:body response)))
    (throw+ "Could not determine url")))

