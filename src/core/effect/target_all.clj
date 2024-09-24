(ns ^:no-doc core.effect.target-all
  (:require [core.ctx :refer :all]
            [core.effect.core :as effect]
            [core.entity :as entity]
            [core.world.ctx :refer [active-entities]]))

(defcomponent :entity-effects {:data [:components-ns :effect.entity]})

; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
; same code as in render entities on world view screens/world
(defn- creatures-in-los-of-player [ctx]
  (->> (active-entities ctx)
       (filter #(:creature/species @%))
       (filter #(entity/line-of-sight? ctx (player-entity* ctx) @%))
       (remove #(:entity/player? @%))))

; TODO targets projectiles with -50% hp !!

(comment
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [ctx @app/state
       targets (creatures-in-los-of-player ctx)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (:position @%)) targets)))

 )

(defcomponent :effect/target-all
  {:data [:map [:entity-effects]]
   :let {:keys [entity-effects]}}
  (info-text [_ ctx]
    "[LIGHT_GRAY]All visible targets[]")

  (applicable? [_ _ctx]
    true)

  (useful? [_ _ctx]
    ; TODO
    false
    )

  (do! [_ {:keys [effect/source] :as ctx}]
    (let [source* @source]
      (apply concat
             (for [target (creatures-in-los-of-player ctx)]
               [[:tx/line-render {:start (:position source*) #_(start-point source* target*)
                                  :end (:position @target)
                                  :duration 0.05
                                  :color [1 0 0 0.75]
                                  :thick? true}]
                ; some sound .... or repeat smae sound???
                ; skill do sound  / skill start sound >?
                ; problem : nested tx/effect , we are still having direction/target-position
                ; at sub-effects
                ; and no more safe - merge
                ; find a way to pass ctx / effect-ctx separate ?
                [:tx/effect {:effect/source source :effect/target target} entity-effects]]))))

  (effect/render [_ g {:keys [effect/source] :as ctx}]
    (let [source* @source]
      (doseq [target* (map deref (creatures-in-los-of-player ctx))]
        (draw-line g
                   (:position source*) #_(start-point source* target*)
                   (:position target*)
                   [1 0 0 0.5])))))
