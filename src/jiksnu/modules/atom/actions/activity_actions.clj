(ns jiksnu.modules.atom.actions.activity-actions)

  (:require [ciste.config :refer [config]]
            [ciste.core :refer [defaction]]
            [ciste.initializer :refer [definitializer]]
            [ciste.model :as cm]
            ;; [clj-tigase.element :as element]
            [clojure.core.incubator :refer [-?> -?>>]]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            ;; [jiksnu.modules.atom.util :as abdera]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.model :as model]
            [jiksnu.model.activity :as model.activity]
            [jiksnu.model.domain :as model.domain]
            [jiksnu.model.user :as model.user]
            [jiksnu.namespace :as ns]
            [jiksnu.ops :as ops]
            [jiksnu.session :as session]
            [jiksnu.templates.actions :as templates.actions]
            [jiksnu.transforms :as transforms]
            [jiksnu.transforms.activity-transforms :as transforms.activity]
            [jiksnu.util :as util]
            [lamina.core :as l]
            [lamina.trace :as trace]
            [slingshot.slingshot :refer [throw+]])
  (:import javax.xml.namespace.QName
           jiksnu.model.Activity
           jiksnu.model.User
           org.apache.abdera.model.Entry
           org.apache.abdera.model.Element)


(def ^QName activity-object-type (QName. ns/as "object-type"))

(defn parse-reply-to
  "extract the ref value of a link and set that as a parent id

This is a byproduct of OneSocialWeb's incorrect use of the ref value"
  [^Element element]
  (let [parent-id (.getAttributeValue element "ref")]
    {:parent-uri parent-id}))

(defn parse-geo
  "extract the latitude and longitude components from a geo element"
  [^Element element]
  (let [coords (.getText element)
        [latitude longitude] (string/split coords #" ")]
    ;; TODO: these should have a common geo property
    {:geo {:latitude latitude :longitude longitude}}))

(defn parse-extension-element
  "parse atom extensions"
  [^Element element]
  #_(let [qname (element/parse-qname (.getQName element))]
    (condp = (:namespace qname)
      ns/as (condp = (:name qname)
              "actor" nil
              ;; "object" (abdera/parse-object-element element)
              nil)

      ns/statusnet (condp = (:name qname)
                     "notice_info" (abdera/parse-notice-info element)
                     nil)

      ns/thr (condp = (:name qname)
               "in-reply-to" (parse-reply-to element)
               nil)

      ns/geo (condp = (:name qname)
               "point" (parse-geo element)
               nil)

      nil)))

(defn get-verb
  "Returns the verb of the entry"
  [^Entry entry]
  #_(-?> entry
       (abdera/get-simple-extension ns/as "verb")
       util/strip-namespaces))

(defn parse-entry
  "initial step of parsing an entry"
  [^Entry entry]
  {:id         (str (.getId entry))
   :url        (str (.getAlternateLinkResolvedHref entry))
   :title      "" #_(.getTitle entry)
   :content    (.getContent entry)
   :published  (.getPublished entry)
   :updated    (.getUpdated entry)
   ;; :links      (abdera/parse-links entry)
   :extensions (.getExtensions entry)})

(defn get-mentioned-uris
  [entry]
  #_(-?>> (concat (.getLinks entry "mentioned")
                (.getLinks entry "ostatus:attention"))
        (map abdera/get-href)
        (filter (complement #{"http://activityschema.org/collection/public"}))
        (into #{})))

(defn ^Activity entry->activity
  "Converts an Abdera entry to the clojure representation of the json
serialization"
  [^Entry entry & [feed source]]
  (dosync
   (ref-set latest-entry entry))
  (trace/trace :entry:parsed entry)
  #_(let [{:keys [extensions content id title published updated links] :as parsed-entry}
        (parse-entry entry)
        original-activity (model.activity/fetch-by-remote-id id)
        verb (get-verb entry)
        parsed-user (let [user-element (abdera/get-author entry feed)]
                      (actions.user/person->user user-element))
        user (actions.user/find-or-create parsed-user)]

    ;; TODO: send parsed user into a channel to be updated once
    (doseq [link (:links parsed-user)]
      (actions.user/add-link user link))

    (let [extension-maps (doall (map parse-extension-element extensions))
          irts (seq (abdera/parse-irts entry))
          mentioned-uris (get-mentioned-uris entry)
          conversation-uris (-?>> (.getLinks entry "ostatus:conversation")
                                  (map abdera/get-href)
                                  (into #{}))
          enclosures (-?> (.getLinks entry "enclosure")
                          (->> (map abdera/parse-link)
                               (into #{})))
          tags (->> entry
                    abdera/parse-tags
                    (filter (complement #{""}))
                    seq)
          object-element (abdera/get-extension entry ns/as "object")
          object-type (-?> (or (-?> object-element
                                    (.getFirstChild activity-object-type))
                               (-?> entry
                                    (.getExtension activity-object-type)))
                           .getText
                           util/strip-namespaces)
          object-id (-?> object-element
                         (.getFirstChild (QName. ns/atom "id")))
          params (apply merge
                        (dissoc parsed-entry :extensions)
                        (when content           {:content (util/sanitize content)})
                        (when updated           {:updated updated})
                        ;; (when (seq recipients) {:recipients (string/join ", " recipients)})
                        (when title             {:title title})
                        (when irts              {:irts irts})
                        (when (seq links)       {:links links})
                        (when (seq conversation-uris)
                          {:conversation-uris conversation-uris})
                        (when (seq mentioned-uris)
                          {:mentioned-uris mentioned-uris})
                        (when (seq enclosures)
                          {:enclosures enclosures})
                        (when (seq tags)
                          {:tags tags})
                        (when verb              {:verb verb})
                        {:id id
                         :author (:_id user)
                         :update-source (:_id source)
                         ;; TODO: try to read
                         :public true
                         :object (merge (when object-type {:type object-type})
                                        (when object-id {:id object-id}))
                         :comment-count (abdera/get-comment-count entry)}
                        extension-maps)]
      (model/map->Activity params))))

