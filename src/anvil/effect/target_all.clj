(ns ^:no-doc anvil.effect.target-all
  (:require [anvil.component :as component]
            [anvil.effect :refer [do-all!]]
            [cdq.context :as world]
            [gdl.context :as c]))

; TODO targets projectiles with -50% hp !!

(defmethods :effects/target-all
  (component/info [_]
    "All visible targets")

  (component/applicable? [_ _]
    true)

  (component/useful? [_ _ _c]
    ; TODO
    false)

  (component/handle [[_ {:keys [entity-effects]}] {:keys [effect/source]} c]
    (let [source* @source]
      (doseq [target (world/creatures-in-los-of-player c)]
        (world/line-render c
                           {:start (:position source*) #_(start-point source* target*)
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
        (do-all! c
                 {:effect/source source :effect/target target}
                 entity-effects))))

  (component/render-effect [_ {:keys [effect/source]} c]
    (let [source* @source]
      (doseq [target* (map deref (world/creatures-in-los-of-player c))]
        (c/line c
                (:position source*) #_(start-point source* target*)
                (:position target*)
                [1 0 0 0.5])))))
