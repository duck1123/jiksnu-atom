(ns jiksnu.modules.web.views.user-views
  (:require [ciste.core :refer [with-format]]
            [ciste.views :refer [defview]]
            [ciste.sections.default :refer [uri index-section show-section]]
            [clj-tigase.element :as element]
            [clojure.tools.logging :as log]
            [hiccup.core :as h]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.ko :refer [*dynamic*]]
            [jiksnu.namespace :as ns]
            [jiksnu.model.webfinger :as model.webfinger]
            [jiksnu.model.user :as model.user]
            [jiksnu.modules.web.sections.user-sections :as sections.user]
            [jiksnu.modules.web.sections :refer [bind-to pagination-links with-page]]
            [ring.util.response :as response]))

;; user-meta

(defview #'actions.user/user-meta :html
  [request user]
  {:template false
   :headers {"Content-Type" "application/xrds+xml"
             "Access-Control-Allow-Origin" "*"}
   :body (h/html (model.webfinger/user-meta user))})

