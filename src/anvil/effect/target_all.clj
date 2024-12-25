(ns ^:no-doc anvil.effect.target-all
  (:require [anvil.component :as component]
            [anvil.effect :refer [do-all!]]
            [anvil.world :as world]
            [gdl.context :as c]))

; TODO targets projectiles with -50% hp !!

(defmethods :effects/target-all
  (component/info [_]
    "All visible targets")

  (component/applicable? [_ _]
    true)

  (component/useful? [_ _]
    ; TODO
    false)

  (component/handle [[_ {:keys [entity-effects]}] {:keys [effect/source]}]
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
                 entity-effects))))

  (component/render-effect [_ {:keys [effect/source]}]
    (let [c (c/get-ctx)
          source* @source]
      (doseq [target* (map deref (world/creatures-in-los-of-player))]
        (c/line c
                (:position source*) #_(start-point source* target*)
                (:position target*)
                [1 0 0 0.5])))))
