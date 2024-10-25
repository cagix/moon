(ns moon.tx.effect
  (:require [moon.component :refer [defc] :as component]
            [moon.effect :as effect]))

(defc :tx/effect
  (component/handle [[_ effect-ctx effect]]
    (effect/with-ctx effect-ctx
      (component/->handle
       (effect/filter-applicable? effect)))))
