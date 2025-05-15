(ns cdq.impl.effects
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.draw :as draw]
            [cdq.rand :refer [rand-int-between]]
            [cdq.raycaster :as raycaster]
            [cdq.timer :as timer]
            [cdq.projectile :as projectile]
            [cdq.utils :refer [defcomponent]]
            [cdq.vector2 :as v]
            [cdq.world :as world]))

(defcomponent :effects/audiovisual
  (effect/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (effect/useful? [_ _]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target-position]}]
    [[:tx/audiovisual target-position audiovisual]]))

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defcomponent :effects/projectile
  ; TODO for npcs need target -- anyway only with direction
  (effect/applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  ; TODO valid params direction has to be  non-nil (entities not los player ) ?
  (effect/useful? [[_ {:keys [projectile/max-range] :as projectile}]
                   {:keys [effect/source effect/target]}]
    (let [source-p (:position @source)
          target-p (:position @target)]
      ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
      (and (not (raycaster/path-blocked? (:raycaster ctx/world) ; TODO test
                                         source-p
                                         target-p
                                         (projectile/size projectile)))
           ; TODO not taking into account body sizes
           (< (v/distance source-p ; entity/distance function protocol EntityPosition
                          target-p)
              max-range))))

  (effect/handle [[_ projectile] {:keys [effect/source effect/target-direction]}]
    [[:tx/spawn-projectile
      {:position (projectile-start-point @source
                                         target-direction
                                         (projectile/size projectile))
       :direction target-direction
       :faction (:entity/faction @source)}
      projectile]]))

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
   [:world/projectile projectile-id ...]
   )
 )

(defcomponent :effects/sound
  (effect/applicable? [_ _ctx]
    true)

  (effect/useful? [_ _]
    false)

  (effect/handle [[_ sound] _ctx]
    [[:tx/sound sound]]))

(defcomponent :effects/spawn
  (effect/applicable? [_ {:keys [effect/source effect/target-position]}]
    (and (:entity/faction @source)
         target-position))

  (effect/handle [[_ {:keys [property/id]}]
                  {:keys [effect/source effect/target-position]}]
    [[:tx/spawn-creature {:position target-position
                          :creature-id id ; already properties/get called through one-to-one, now called again.
                          :components {:entity/fsm {:fsm :fsms/npc
                                                    :initial-state :npc-idle}
                                       :entity/faction (:entity/faction @source)}}]]))

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (world/creatures-in-los-of-player ctx/world)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (:position @%)) targets)))

 )

(defcomponent :effects/target-all
  ; TODO targets projectiles with -50% hp !!
  (effect/applicable? [_ _]
    true)

  (effect/useful? [_ _]
    ; TODO
    false)

  (effect/handle [[_ {:keys [entity-effects]}] {:keys [effect/source]}]
    (let [source* @source]
      (apply concat
             (for [target (world/creatures-in-los-of-player ctx/world)]
               [[:tx/spawn-line {:start (:position source*) #_(start-point source* target*)
                                 :end (:position @target)
                                 :duration 0.05
                                 :color [1 0 0 0.75]
                                 :thick? true}]
                ; some sound .... or repeat smae sound???
                ; skill do sound  / skill start sound >?
                ; problem : nested effect/do-all! , we are still having direction/target-position
                ; at sub-effects
                ; and no more safe - merge
                ; find a way to pass ctx / effect-ctx separate ?
                [:tx/effect {:effect/source source :effect/target target} entity-effects]]))))

  (effect/render [_ {:keys [effect/source]}]
    (let [source* @source]
      (doseq [target* (map deref (world/creatures-in-los-of-player ctx/world))]
        (draw/line (:position source*) #_(start-point source* target*)
                   (:position target*)
                   [1 0 0 0.5])))))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (:position entity)
         (v/scale (entity/direction entity target*)
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (entity/direction entity target*)
                  maxrange)))

(defcomponent :effects/target-entity
  (effect/applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as effect-ctx}]
    (and target
         (seq (effect/filter-applicable? effect-ctx entity-effects))))

  (effect/useful? [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
    (entity/in-range? @source @target maxrange))

  (effect/handle [[_ {:keys [maxrange entity-effects]}]
                  {:keys [effect/source effect/target] :as effect-ctx}]
    (let [source* @source
          target* @target]
      (if (entity/in-range? source* target* maxrange)
        [[:tx/spawn-line {:start (start-point source* target*)
                          :end (:position target*)
                          :duration 0.05
                          :color [1 0 0 0.75]
                          :thick? true}]
         [:tx/effect effect-ctx entity-effects]]
        [[:tx/audiovisual
          (end-point source* target* maxrange)
          (db/build ctx/db :audiovisuals/hit-ground)]])))

  (effect/render [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
    (when target
      (let [source* @source
            target* @target]
        (draw/line (start-point source* target*)
                   (end-point source* target* maxrange)
                   (if (entity/in-range? source* target* maxrange)
                     [1 0 0 0.5]
                     [1 1 0 0.5]))))))

(defcomponent :effects.target/audiovisual
  (effect/applicable? [_ {:keys [effect/target]}]
    target)

  (effect/useful? [_ _]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target]}]
    [[:tx/audiovisual (:position @target) audiovisual]]))

(defcomponent :effects.target/convert
  (effect/applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (effect/handle [_ {:keys [effect/source effect/target]}]
    [[:tx/assoc target :entity/faction (:entity/faction @source)]]))

(defn- effective-armor-save [source* target*]
  (max (- (or (entity/stat target* :entity/armor-save) 0)
          (or (entity/stat source* :entity/armor-pierce) 0))
       0))

(comment
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )


(defn- armor-saves? [source* target*]
  (< (rand) (effective-armor-save source* target*)))

(defcomponent :effects.target/damage
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/hp @target)))

  (effect/handle [[_ damage]
                  {:keys [effect/source effect/target]}]
    (let [source* @source
          target* @target
          hp (entity/hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*) ; TODO BUG ALWAYS ARMOR ?
       [[:tx/add-text-effect target "[WHITE]ARMOR"]]

       :else
       (let [min-max (:damage/min-max (entity/damage source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         [[:tx/assoc-in target [:entity/hp 0] new-hp-val]
          [:tx/audiovisual (:position target*) (db/build ctx/db :audiovisuals/damage)]
          [:tx/event target (if (zero? new-hp-val) :kill :alert)]
          [:tx/add-text-effect target (str "[RED]" dmg-amount "[]")]])))))

(defcomponent :effects.target/kill
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [_ {:keys [effect/target]}]
    [[:tx/event target :kill]]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defcomponent :effects.target/melee-damage
  (effect/applicable? [_ {:keys [effect/source] :as ctx}]
    (effect/applicable? (melee-damage-effect @source) ctx))

  (effect/handle [_ {:keys [effect/source] :as ctx}]
    (effect/handle (melee-damage-effect @source) ctx)))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defcomponent :effects.target/spiderweb
    (effect/applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (effect/handle [_ {:keys [effect/target]}]
      (when-not (:entity/temp-modifier @target)
        [[:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                  :counter (timer/create ctx/elapsed-time duration)}]
         [:tx/mod-add target modifiers]]))))

(defcomponent :effects.target/stun
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [[_ duration] {:keys [effect/target]}]
    [[:tx/event target :stun duration]]))
