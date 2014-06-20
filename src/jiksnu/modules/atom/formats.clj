(ns jiksnu.modules.atom.formats
  (:require [clojure.tools.logging :as log]
            [jiksnu.modules.atom.util :as abdera]))

(defmethod format-as :atom
  [format request response]
  (let [atom-map (-> (:body response)
                     (assoc :title (:title response)))]
    (-> response
        (assoc :body (abdera/make-feed atom-map)))))
