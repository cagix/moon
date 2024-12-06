(ns forge.effects
  (:require [forge.core :refer :all]
            [forge.system :refer [defmethods]]))

(comment

 ; TODO check this
 (def ^:private effect
   {:required [#'applicable?
               #'handle]
    :optional [#'useful?
               #'render-effect]})

 )

#_(defn- stat-k [effect-k]
    (keyword "stats" (name effect-k)))

#_(defmethods :effects.target/hp
  (info [[k ops]]
    (ops/info ops k))

  (applicable? [[k _] {:keys [effect/source effect/target]}]
    (and effect/target
         (mods/value @target (stat-k k))))

  (useful? [_ _]
    true)

  (handle [[k operations] {:keys [effect/source effect/target]}]
    (let [stat-k (stat-k k)]
      (when-let [value (mods/value @target stat-k)]
        (swap! target assoc stat-k (ops/apply operations value))))))

(defmethods :effects.target/audiovisual
  (applicable? [_ {:keys [effect/target]}]
    target)

  (useful? [_ _]
    false)

  (handle [[_ audiovisual] {:keys [effect/target]}]
    (spawn-audiovisual (:position @target) audiovisual)))

(defmethods :effects.target/convert
  (applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (e-enemy @source))))

  (handle [_ {:keys [effect/source effect/target]}]
    (swap! target assoc :entity/faction (:entity/faction @source))))

(defn- effective-armor-save [source* target*]
  (max (- (or (e-stat target* :entity/armor-save) 0)
          (or (e-stat source* :entity/armor-pierce) 0))
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
          hp (hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*)
       (swap! target add-text-effect "[WHITE]ARMOR")

       :else
       (let [min-max (:damage/min-max (damage-mods source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         (swap! target assoc-in [:entity/hp 0] new-hp-val)
         (spawn-audiovisual (:position target*)
                            (build :audiovisuals/damage))
         (send-event target (if (zero? new-hp-val) :kill :alert))
         (swap! target add-text-effect (str "[RED]" dmg-amount "[]")))))))

(defmethods :effects.target/kill
  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [_ {:keys [effect/target]}]
    (send-event target :kill)))

(defn- entity->melee-damage [entity]
  (let [strength (or (e-stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defmethods :effects.target/melee-damage
  (applicable? [_ {:keys [effect/source] :as ctx}]
    (applicable? [:effects.target/damage (entity->melee-damage @source)] ctx))

  (handle [_ {:keys [effect/source] :as ctx}]
    (handle [:effects.target/damage (entity->melee-damage @source)] ctx)))

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
        (swap! target add-mods modifiers)))))

(defmethods :effects.target/stun
  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [[_ duration] {:keys [effect/target]}]
    (send-event target :stun duration)))

(defn- in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v-distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v-add (:position entity)
         (v-scale (e-direction entity target*)
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v-add (start-point entity target*)
         (v-scale (e-direction entity target*)
                  maxrange)))

(defmethods :effects/target-entity
  (applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
    (and target
         (effects-applicable? ctx entity-effects)))

  (useful? [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
    (in-range? @source @target maxrange))

  (handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx}]
    (let [source* @source
          target* @target]
      (if (in-range? source* target* maxrange)
        (do
         (spawn-line-render {:start (start-point source* target*)
                             :end (:position target*)
                             :duration 0.05
                             :color [1 0 0 0.75]
                             :thick? true})
         (effects-do! ctx entity-effects))
        (spawn-audiovisual (end-point source* target* maxrange)
                           (build :audiovisuals/hit-ground)))))

  (render-effect [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
    (when target
      (let [source* @source
            target* @target]
        (draw-line (start-point source* target*)
                   (end-point source* target* maxrange)
                   (if (in-range? source* target* maxrange)
                     [1 0 0 0.5]
                     [1 1 0 0.5]))))))

(defn- projectile-start-point [entity direction size]
  (v-add (:position entity)
         (v-scale direction
                  (+ (:radius entity) size 0.1))))

; TODO for npcs need target -- anyway only with direction
(defmethods :effects/projectile
  (applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  ; TODO valid params direction has to be  non-nil (entities not los player ) ?
  (useful? [[_ {:keys [projectile/max-range] :as projectile}]
            {:keys [effect/source effect/target]}]
    (let [source-p (:position @source)
          target-p (:position @target)]
      ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
      (and (not (path-blocked? ; TODO test
                               source-p
                               target-p
                               (projectile-size projectile)))
           ; TODO not taking into account body sizes
           (< (v-distance source-p ; entity/distance function protocol EntityPosition
                          target-p)
              max-range))))

  (handle [[_ projectile] {:keys [effect/source effect/target-direction]}]
    (play-sound "bfxr_waypointunlock")
    (spawn-projectile {:position (projectile-start-point @source
                                                         target-direction
                                                         (projectile-size projectile))
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
    (spawn-creature {:position target-position
                     :creature-id id ; already properties/get called through one-to-one, now called again.
                     :components {:entity/fsm {:fsm :fsms/npc
                                               :initial-state :npc-idle}
                                  :entity/faction (:entity/faction @source)}})))

; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
; same code as in render entities on world view screens/world
(defn- creatures-in-los-of-player []
  (->> (active-entities)
       (filter #(:entity/species @%))
       (filter #(line-of-sight? @player-eid @%))
       (remove #(:entity/player? @%))))

; TODO targets projectiles with -50% hp !!

(comment
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

  (useful? [_ _]
    ; TODO
    false
    )

  (handle [[_ {:keys [entity-effects]}] {:keys [effect/source]}]
    (let [source* @source]
      (doseq [target (creatures-in-los-of-player)]
        (spawn-line-render {:start (:position source*) #_(start-point source* target*)
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
        (effects-do! {:effect/source source :effect/target target}
                     entity-effects))))

  (render-effect [_ {:keys [effect/source]}]
    (let [source* @source]
      (doseq [target* (map deref (creatures-in-los-of-player))]
        (draw-line (:position source*) #_(start-point source* target*)
                   (:position target*)
                   [1 0 0 0.5])))))
