(ns jiksnu.modules.atom.views.stream-views
  (:require [ciste.config :refer [config]]
            [ciste.core :refer [with-format]]
            [ciste.views :refer [apply-view defview]]
            [ciste.sections.default :refer [full-uri index-line index-section show-section]]
            [clojure.tools.logging :as log]
            [jiksnu.actions.activity-actions :as actions.activity]
            jiksnu.actions.stream-actions
            [jiksnu.model :as model]
            [jiksnu.namespace :as ns]
            [jiksnu.session :refer [current-user]]))

(defview #'mentions-timeline :atom
  [request activities]
  {:body
   [:statuses (map index-line activities)]})

(defview #'public-timeline :atom
  [request {:keys [items] :as page}]
  (let [self (str "http://" (config :domain) "/api/statuses/public_timeline.atom")]
    {:headers {"Content-Type" "application/xml"}
     :template false
     :title "Public Activities"
     :body {:subtitle "All activities posted"
            :id self
            :links [{:href (str "http://" (config :domain) "/")
                     :rel "alternate"
                     :type "text/html"}
                    {:href self
                     :rel "self"
                     :type "application/atom+xml"}]
            :updated (:updated (first items))
            :entries (index-section items page)}}))

;; user-timeline

(defview #'user-timeline :atom
  [request [user {activities :items :as page}]]
  {:headers {"Content-Type" "application/xml"}
   :template false
   :title (str (:username user) " timeline")
   :body {
          ;; TODO: pick these up from maven
          :generator {:uri "http://jiksnu.com/"
                      :name "Jiksnu"
                      :version "0.1.0-SNAPSHOT"}
          :subtitle (str "Updates from " (:username user) " on " (:domain user))
          :id (str "http://" (config :domain) "/api/statuses/user_timeline/" (:_id user) ".atom")
          :links
          (let [d (config :domain)
                id (:_id user)]
            [{:href (full-uri user)
              :rel "alternate"
              :type "text/html"}
             {:href (format "http://%s/api/statuses/user_timeline/%s.atom" d id)
              :rel "self"
              :type "application/atom+xml"}
             {:href (format "http://%s/main/push/hub" d)
              :rel "hub"}
             {:href (format "http://%s/main/salmon/user/%s" d id)
              :rel "salmon"}
             {:href (format "http://%s/main/salmon/user/%s" d id)
              :rel "http://salmon-protocol.org/ns/salmon-replies"}
             {:href (format "http://%s/main/salmon/user/%s" d id)
              :rel "http://salmon-protocol.org/ns/salmon-mention"}])
          :author (show-section user)
          :updated (:updated (first activities))
          :entries (map show-section activities)}})

