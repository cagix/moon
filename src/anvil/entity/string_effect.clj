(ns ^:no-doc anvil.entity.string-effect
  (:require [cdq.context :refer [stopped?]]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :entity/string-effect
  (component/tick [[k {:keys [counter]}] eid c]
    (when (stopped? c counter)
      (swap! eid dissoc k))))
