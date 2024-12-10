(ns anvil.effect
  (:require [anvil.audio :refer [play-sound]]
            [anvil.body :as body]
            [anvil.damage :as damage]
            [anvil.db :as db]
            [anvil.entity :as entity :refer [creatures-in-los-of-player]]
            [anvil.faction :as faction]
            [anvil.fsm :as fsm]
            [anvil.hitpoints :as hp]
            [anvil.modifiers :as mods]
            [anvil.stat :as stat]
            [anvil.string-effect :as string-effect]
            [anvil.time :refer [timer]]
            [clojure.component :refer [defsystem]]
            [clojure.gdx.math.vector2 :as v]
            [clojure.rand :refer [rand-int-between]]
            [clojure.utils :refer [defmethods]]))

(defsystem applicable?)

(defn filter-applicable? [ctx effects]
  (filter #(applicable? % ctx) effects))

(defn some-applicable? [ctx effects]
  (seq (filter-applicable? ctx effects)))

(defsystem handle)

(defn do-all! [ctx effects]
  (run! #(handle % ctx)
        (filter-applicable? ctx effects)))

(defmethods :effects.target/audiovisual
  (applicable? [_ {:keys [effect/target]}]
    target)

  (handle [[_ audiovisual] {:keys [effect/target]}]
    (entity/audiovisual (:position @target) audiovisual)))

(defmethods :effects.target/convert
  (applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (faction/enemy @source))))

  (handle [_ {:keys [effect/source effect/target]}]
    (swap! target assoc :entity/faction (:entity/faction @source))))

(defn- effective-armor-save [source* target*]
  (max (- (or (stat/->value target* :entity/armor-save) 0)
          (or (stat/->value source* :entity/armor-pierce) 0))
       0))

(comment
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(defn- armor-saves? [source* target*]
  (< (rand) (effective-armor-save source* target*)))

(defmethods :effects.target/damage
  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/hp @target)))

  (handle [[_ damage] {:keys [effect/source effect/target]}]
    (let [source* @source
          target* @target
          hp (hp/->value target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*)
       (swap! target string-effect/add "[WHITE]ARMOR")

       :else
       (let [min-max (:damage/min-max (damage/->value source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         (swap! target assoc-in [:entity/hp 0] new-hp-val)
         (entity/audiovisual (:position target*)
                             (db/build :audiovisuals/damage))
         (fsm/event target (if (zero? new-hp-val) :kill :alert))
         (swap! target string-effect/add (str "[RED]" dmg-amount "[]")))))))

(defmethods :effects.target/kill
  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [_ {:keys [effect/target]}]
    (fsm/event target :kill)))

(defn- entity->melee-damage [entity]
  (let [strength (or (stat/->value entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defmethods :effects.target/melee-damage
  (applicable? [_ {:keys [effect/source] :as ctx}]
    (applicable? (damage-effect @source) ctx))

  (handle [_ {:keys [effect/source] :as ctx}]
    (handle (damage-effect @source) ctx)))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]

  (defmethods :effects.target/spiderweb
    (applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (handle [_ {:keys [effect/target]}]
      (when-not (:entity/temp-modifier @target)
        (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                   :counter (timer duration)})
        (swap! target mods/add modifiers)))))

(defmethods :effects.target/stun
  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [[_ duration] {:keys [effect/target]}]
    (fsm/event target :stun duration)))

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defmethods :effects/projectile
  ; TODO for npcs need target -- anyway only with direction
  (applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  (handle [[_ projectile] {:keys [effect/source effect/target-direction]}]
    (play-sound "bfxr_waypointunlock")
    (entity/projectile {:position (projectile-start-point @source
                                                          target-direction
                                                          (entity/projectile-size projectile))
                        :direction target-direction
                        :faction (:entity/faction @source)}
                       projectile)))

(comment
 ; mass shooting
 (for [direction (map math.vector/normalise
                      [[1 0]
                       [1 1]
                       [1 -1]
                       [0 1]
                       [0 -1]
                       [-1 -1]
                       [-1 1]
                       [-1 0]])]
   [:tx/projectile projectile-id ...]
   )
 )

; "https://github.com/damn/core/issues/29"
(defmethods :effects/spawn
  (applicable? [_ {:keys [effect/source effect/target-position]}]
    (and (:entity/faction @source)
         target-position))

  (handle [[_ {:keys [property/id]}]
                {:keys [effect/source effect/target-position]}]
    (play-sound "bfxr_shield_consume")
    (entity/creature {:position target-position
                      :creature-id id ; already properties/get called through one-to-one, now called again.
                      :components {:entity/fsm {:fsm :fsms/npc
                                                :initial-state :npc-idle}
                                   :entity/faction (:entity/faction @source)}})))



; TODO targets projectiles with -50% hp !!

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (:position @%)) targets)))

 )

(defmethods :effects/target-all
  (applicable? [_ _]
    true)

  (handle [[_ {:keys [entity-effects]}] {:keys [effect/source]}]
    (let [source* @source]
      (doseq [target (creatures-in-los-of-player)]
        (entity/line-render {:start (:position source*) #_(start-point source* target*)
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

(defn in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn start-point [entity target*]
  (v/add (:position entity)
         (v/scale (body/direction entity target*)
                  (:radius entity))))

(defn end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (body/direction entity target*)
                  maxrange)))

(defmethods :effects/target-entity
  (applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
    (and target
         (seq (filter-applicable? ctx entity-effects))))

  (handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx}]
    (let [source* @source
          target* @target]
      (if (in-range? source* target* maxrange)
        (do
         (entity/line-render {:start (start-point source* target*)
                              :end (:position target*)
                              :duration 0.05
                              :color [1 0 0 0.75]
                              :thick? true})
         (do-all! ctx entity-effects))
        (entity/audiovisual (end-point source* target* maxrange)
                            (db/build :audiovisuals/hit-ground))))))
