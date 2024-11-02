(ns moon.tx.effect
  (:require [moon.component :as component]
            [moon.effect :as effect]))

(defmethods :tx/effect
  (component/handle [[_ effect-ctx effect]]
    (effect/with-ctx effect-ctx
      (component/->handle
       (effect/filter-applicable? effect)))))
