(ns jiksnu.modules.atom.actions.stream-actions
  (:require [ciste.core :only [defaction]]
            [ciste.sections.default :only [show-section]]
            [clojure.core.incubator :only [-?> -?>>]]
            [clojure.tools.logging :as log]
            [jiksnu.modules.atom.util :as abdera]
            [jiksnu.actions.feed-source-actions :as actions.feed-source]
            [jiksnu.channels :as ch]
            [slingshot.slingshot :only [throw+]]))


(defaction callback-publish
  [feed]
  (if-let [topic (-?> feed (abdera/rel-filter-feed "self")
                      first abdera/get-href)]
    (if-let [source (actions.feed-source/find-or-create {:topic topic})]
      (do
        (actions.feed-source/process-feed source feed)
        true)
      (throw+ "could not create source"))
    (throw+ "Could not determine topic")))

