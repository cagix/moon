(ns anvil.effect.target-all
  (:require [anvil.component :refer [applicable? handle]]
            [anvil.effect :refer [do-all!]]
            [anvil.world :as world]
            [gdl.utils :refer [defmethods]]))

; TODO targets projectiles with -50% hp !!

(defmethods :effects/target-all
  (applicable? [_ _]
    true)

  (handle [[_ {:keys [entity-effects]}] {:keys [effect/source]}]
    (let [source* @source]
      (doseq [target (world/creatures-in-los-of-player)]
        (world/line-render {:start (:position source*) #_(start-point source* target*)
                            :end (:position @target)
                            :duration 0.05
                            :color [1 0 0 0.75]
                            :thick? true})
        ; some sound .... or repeat smae sound???
        ; skill do sound  / skill start sound >?
        ; problem : nested tx/effect , we are still having direction/target-position
        ; at sub-effects
        ; and no more safe - merge
        ; find a way to pass ctx / effect-ctx separate ?
        (do-all! {:effect/source source :effect/target target}
                 entity-effects)))))
