(ns jiksnu.modules.web.filters.stream-filters
  (:require [ciste.filters :refer [deffilter]]
            [clojure.tools.logging :as log]
            [jiksnu.actions.stream-actions :as actions.stream]
            [jiksnu.modules.atom.util :as abdera]
            [lamina.trace :as trace]))

(deffilter #'actions.stream/callback-publish :http
  [action request]
  (action (abdera/stream->feed (:body request))))

