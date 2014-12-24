(ns jiksnu.modules.web.views.user-views
  (:require [ciste.views :refer [defview]]
            [hiccup.core :as h]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.model.webfinger :as model.webfinger]))

(defview #'actions.user/user-meta :html
  [request user]
  {:template false
   :headers {"Content-Type" "application/xrds+xml"
             "Access-Control-Allow-Origin" "*"}
   :body (h/html (model.webfinger/user-meta user))})
