(ns ^:no-doc anvil.entity.delete-after-duration
  (:require [cdq.context :refer [timer finished-ratio]]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.utils :refer [readable-number]]))

(defcomponent :entity/delete-after-duration
  (component/create [[_ duration] c]
    (timer c duration))

  (component/info [counter c]
    (str "Remaining: " (readable-number (finished-ratio c counter)) "/1")))
