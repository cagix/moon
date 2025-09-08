(ns cdq.game.effects
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.faction :as faction]
            [cdq.raycaster :as raycaster]
            [cdq.stats :as modifiers]
            [cdq.timer :as timer]
            [cdq.gdx.math.vector2 :as v]))

(defn- entity->melee-damage [{:keys [creature/stats]}]
  (let [strength (or (modifiers/get-stat-value stats :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (entity/position @%)) targets)))

 )

(defn- creatures-in-los-of
  [{:keys [ctx/active-entities
           ctx/raycaster]}
   entity]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(raycaster/line-of-sight? raycaster entity @%))
       (remove #(:entity/player? @%))))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (entity/position entity)
         (v/scale (v/direction (entity/position entity)
                               (entity/position target*))
                  (/ (:body/width (:entity/body entity)) 2))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (v/direction (entity/position entity)
                               (entity/position target*))
                  maxrange)))

(defn- in-range? [entity target* maxrange]
  (< (- (float (v/distance (entity/position entity)
                           (entity/position target*)))
        (float (/ (:body/width (:entity/body entity))  2))
        (float (/ (:body/width (:entity/body target*)) 2)))
     (float maxrange)))

(defn- proj-start-point [entity direction size]
  (v/add (entity/position entity)
         (v/scale direction
                  (+ (/ (:body/width (:entity/body entity)) 2) size 0.1))))
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

(defn init! []
  (.bindRoot #'cdq.effect/k->method-map
             {:effects/audiovisual {:applicable? (fn [_ {:keys [effect/target-position]}]
                                                   target-position)
                                    :useful? (fn [_ _effect-ctx _ctx]
                                               false)
                                    :handle (fn [[_ audiovisual] {:keys [effect/target-position]} _ctx]
                                              [[:tx/audiovisual target-position audiovisual]])}
              :effects/projectile { ; TODO for npcs need target -- anyway only with direction
                                   :applicable? (fn [_ {:keys [effect/target-direction]}]
                                                  target-direction) ; faction @ source also ?

                                   ; TODO valid params direction has to be  non-nil (entities not los player ) ?
                                   :useful? (fn [[_ {:keys [projectile/max-range] :as projectile}]
                                                 {:keys [effect/source effect/target]}
                                                 ctx]
                                              (let [source-p (entity/position @source)
                                                    target-p (entity/position @target)]
                                                ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
                                                (and (not (raycaster/path-blocked? (:ctx/raycaster ctx) source-p target-p (:projectile/size projectile)))
                                                     ; TODO not taking into account body sizes
                                                     (< (v/distance source-p ; entity/distance function protocol EntityPosition
                                                                    target-p)
                                                        max-range))))

                                   :handle (fn [[_ projectile] {:keys [effect/source effect/target-direction]} _ctx]
                                             [[:tx/spawn-projectile
                                               {:position (proj-start-point @source
                                                                            target-direction
                                                                            (:projectile/size projectile))
                                                :direction target-direction
                                                :faction (:entity/faction @source)}
                                               projectile]])}
              :effects/sound {:applicable? (fn [_ _ctx] true)
                              :useful? (fn [_ _effect-ctx _ctx] false)
                              :handle (fn [[_ sound] _effect-ctx _ctx]
                                        [[:tx/sound sound]])}

              :effects/spawn {:applicable? (fn [_ {:keys [effect/source effect/target-position]}]
                                             (and (:entity/faction @source)
                                                  target-position))

                              :handle (fn [[_ {:keys [property/id] :as property}]
                                           {:keys [effect/source effect/target-position]}
                                           _ctx]
                                        [[:tx/spawn-creature {:position target-position
                                                              :creature-property property
                                                              :components {:entity/fsm {:fsm :fsms/npc
                                                                                        :initial-state :npc-idle}
                                                                           :entity/faction (:entity/faction @source)}}]])}

              :effects/target-all {
                                   ; TODO targets projectiles with -50% hp !!
                                   :applicable? (fn [_ _]
                                                  true)

                                   ; TODO
                                   :useful? (fn [_ _effect-ctx _ctx]
                                              false)

                                   :handle (fn [[_ {:keys [entity-effects]}] {:keys [effect/source]} ctx]
                                             (let [source* @source]
                                               (apply concat
                                                      (for [target (creatures-in-los-of ctx source*)]
                                                        [[:tx/spawn-line
                                                          {:start (:body/position (:entity/body source*)) #_(start-point source* target*)
                                                           :end (:body/position (:entity/body @target))
                                                           :duration 0.05
                                                           :color [1 0 0 0.75]
                                                           :thick? true}]
                                                         [:tx/effect
                                                          {:effect/source source
                                                           :effect/target target}
                                                          entity-effects]]))))

                                   :render (fn [_ {:keys [effect/source]} ctx]
                                             (let [source* @source]
                                               (for [target* (map deref (creatures-in-los-of ctx source*))]
                                                 [:draw/line
                                                  (:body/position (:entity/body source*)) #_(start-point source* target*)
                                                  (:body/position (:entity/body target*))
                                                  [1 0 0 0.5]])))
                                   }

              :effects/target-entity {
                                      :applicable? (fn [[_ {:keys [entity-effects]}] {:keys [effect/target] :as effect-ctx}]
                                                     (and target
                                                          (seq (effect/filter-applicable? effect-ctx entity-effects))))

                                      :useful? (fn [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _ctx]
                                                 (in-range? @source @target maxrange))

                                      :handle (fn [[_ {:keys [maxrange entity-effects]}]
                                                   {:keys [effect/source effect/target] :as effect-ctx}
                                                   _ctx]
                                                (let [source* @source
                                                      target* @target]
                                                  (if (in-range? source* target* maxrange)
                                                    [[:tx/spawn-line {:start (start-point source* target*)
                                                                      :end (entity/position target*)
                                                                      :duration 0.05
                                                                      :color [1 0 0 0.75]
                                                                      :thick? true}]
                                                     [:tx/effect effect-ctx entity-effects]]
                                                    [[:tx/audiovisual
                                                      (end-point source* target* maxrange)
                                                      :audiovisuals/hit-ground]])))

                                      :render (fn [[_ {:keys [maxrange]}]
                                                   {:keys [effect/source effect/target]}
                                                   _ctx]
                                                (when target
                                                  (let [source* @source
                                                        target* @target]
                                                    [[:draw/line
                                                      (start-point source* target*)
                                                      (end-point source* target* maxrange)
                                                      (if (in-range? source* target* maxrange)
                                                        [1 0 0 0.5]
                                                        [1 1 0 0.5])]])))
                                      }

              :effects.target/audiovisual {
                                           :applicable? (fn [_ {:keys [effect/target]}]
                                                          target)

                                           :useful? (fn [_ _effect-ctx _ctx]
                                                      false)

                                           :handle (fn [[_ audiovisual] {:keys [effect/target]} _ctx]
                                                     [[:tx/audiovisual (entity/position @target) audiovisual]])
                                           }

              :effects.target/convert {

                                       :applicable? (fn [_ {:keys [effect/source effect/target]}]
                                                      (and target
                                                           (= (:entity/faction @target)
                                                              (faction/enemy (:entity/faction @source)))))

                                       :handle (fn [_ {:keys [effect/source effect/target]} _ctx]
                                                 [[:tx/assoc target :entity/faction (:entity/faction @source)]])
                                       }

              :effects.target/damage {

                                      :applicable? (fn [_ {:keys [effect/target]}]
                                                     (and target
                                                          #_(:entity/hp @target))) ; not exist anymore ... bugfix .... -> is 'creature?'

                                      :handle (fn [[_ damage]
                                                   {:keys [effect/source effect/target]}
                                                   _ctx]
                                                [[:tx/deal-damage source target damage]])
                                      }

              :effects.target/kill {:applicable? (fn [_ {:keys [effect/target]}]
                                                   (and target
                                                        (:entity/fsm @target)))

                                    :handle (fn [_ {:keys [effect/target]} _ctx]
                                              [[:tx/event target :kill]])}

              :effects.target/melee-damage {
                                            :applicable? (fn [_ {:keys [effect/source] :as effect-ctx}]
                                                           (effect/applicable? (melee-damage-effect @source) effect-ctx))

                                            :handle (fn [_ {:keys [effect/source] :as effect-ctx} ctx]
                                                      (effect/handle (melee-damage-effect @source) effect-ctx ctx))
                                            }

              :effects.target/spiderweb (let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
                                              duration 5]

                                          {                                :applicable? (fn [_ _]
                                                                                          ; ?
                                                                                          true)

                                           ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
                                           :handle (fn [_
                                                        {:keys [effect/target]}
                                                        {:keys [ctx/elapsed-time]}]
                                                     (when-not (:entity/temp-modifier @target)
                                                       [[:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                                                                 :counter (timer/create elapsed-time duration)}]
                                                        [:tx/mod-add target modifiers]]))
                                           })

              :effects.target/stun {
                                    :applicable? (fn [_ {:keys [effect/target]}]
                                                   (and target
                                                        (:entity/fsm @target)))

                                    :handle (fn [[_ duration] {:keys [effect/target]} _ctx]
                                              [[:tx/event target :stun duration]])
                                    }}))
