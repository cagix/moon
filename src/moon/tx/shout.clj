(ns moon.tx.shout
  (:require [moon.body :as body]
            [moon.world.time :refer [timer]]))

(defn handle [[_ position faction delay-seconds]]
  [[:e/create
    position
    body/effect-body-props
    {:entity/alert-friendlies-after-duration
     {:counter (timer delay-seconds)
      :faction faction}}]])
