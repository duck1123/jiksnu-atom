(ns jiksnu.modules.core.filters.user-filters
  (:require [ciste.config :refer [config]]
            [ciste.filters :refer [deffilter]]
            [clojure.tools.logging :as log]
            [jiksnu.actions.user-actions :as actions.user]
            [jiksnu.model.user :as model.user]
            [jiksnu.modules.core.filters :refer [parse-page parse-sorting]]
            [jiksnu.session :as session]
            [jiksnu.util :as util]
            [slingshot.slingshot :refer [throw+]]))

;; user-meta

(deffilter #'actions.user/user-meta :http
  [action request]
  (->> request :params :uri
       util/split-uri
       (apply model.user/get-user)
       action))

;; (deffilter #'actions.user/update-hub :http
;;   [action request]
;;   (let [{params :params} request
;;         {username :id} params
;;         user (model.user/fetch-by-id username)]
;;     (action user)))

