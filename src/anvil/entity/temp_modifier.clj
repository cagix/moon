(ns ^:no-doc anvil.entity.temp-modifier
  (:require [cdq.context :refer [finished-ratio]]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.utils :refer [readable-number]]))

(defcomponent :entity/temp-modifier
  (component/info [[_ {:keys [counter]}] c]
    (str "Spiderweb - remaining: " (readable-number (finished-ratio c counter)) "/1")))
