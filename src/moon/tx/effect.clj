(ns moon.tx.effect
  (:require [moon.component :as component]
            [moon.effect :as effect]))

(defn handle [[_ effect-ctx effect]]
  (effect/with-ctx effect-ctx
    (component/->handle
     (effect/filter-applicable? effect))))
