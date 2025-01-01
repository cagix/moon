(ns ^:no-doc anvil.entity.fsm
  (:require [clojure.component :as component :refer [defcomponent]]))

(defcomponent :entity/fsm
  (component/info [[_ fsm] _c]
    (str "State: " (name (:state fsm)))))
