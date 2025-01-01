(ns cdq.effect
  (:require [anvil.effect :refer [do-all! filter-applicable?]]
            [anvil.entity :as entity]
            [cdq.context :as world]
            [clojure.gdx :refer [play]]
            [clojure.component :as component :refer [defcomponent]]
            [gdl.context :as c]
            [gdl.math.vector :as v]))

(defcomponent :effects/audiovisual
  (component/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (component/useful? [_ _ _c]
    false)

  (component/handle [[_ audiovisual] {:keys [effect/target-position]} c]
    (world/audiovisual c target-position audiovisual)))

; "https://github.com/damn/core/issues/29"
(defcomponent :effects/spawn
  (component/applicable? [_ {:keys [effect/source effect/target-position]}]
    (and (:entity/faction @source)
         target-position))

  (component/handle [[_ {:keys [property/id]}]
                     {:keys [effect/source effect/target-position]}
                     c]
    (world/creature c
                    {:position target-position
                     :creature-id id ; already properties/get called through one-to-one, now called again.
                     :components {:entity/fsm {:fsm :fsms/npc
                                               :initial-state :npc-idle}
                                  :entity/faction (:entity/faction @source)}})))

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (world/creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (:position @%)) targets)))

 )

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defcomponent :effects/projectile
  ; TODO for npcs need target -- anyway only with direction
  (component/applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  ; TODO valid params direction has to be  non-nil (entities not los player ) ?
  (component/useful? [[_ {:keys [projectile/max-range] :as projectile}]
                      {:keys [effect/source effect/target]}
                      c]
    (let [source-p (:position @source)
          target-p (:position @target)]
      ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
      (and (not (world/path-blocked? c ; TODO test
                                     source-p
                                     target-p
                                     (world/projectile-size projectile)))
           ; TODO not taking into account body sizes
           (< (v/distance source-p ; entity/distance function protocol EntityPosition
                          target-p)
              max-range))))

  (component/handle [[_ projectile] {:keys [effect/source effect/target-direction]} c]
    (world/projectile c
                      {:position (projectile-start-point @source
                                                         target-direction
                                                         (world/projectile-size projectile))
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

(defcomponent :effects/sound
  (component/applicable? [_ _ctx]
    true)

  (component/useful? [_ _ _c]
    false)

  (component/handle [[_ sound] _ctx c]
    (play sound)))

; TODO targets projectiles with -50% hp !!

(defcomponent :effects/target-all
  (component/info [_ _c]
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

(defn- in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

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
  (component/applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
    (and target
         (seq (filter-applicable? ctx entity-effects))))

  (component/useful?  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _c]
    (in-range? @source @target maxrange))

  (component/handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx} c]
    (let [source* @source
          target* @target]
      (if (in-range? source* target* maxrange)
        (do
         (world/line-render c
                            {:start (start-point source* target*)
                             :end (:position target*)
                             :duration 0.05
                             :color [1 0 0 0.75]
                             :thick? true})
         (do-all! c ctx entity-effects))
        (world/audiovisual c
                           (end-point source* target* maxrange)
                           (c/build c :audiovisuals/hit-ground)))))

  (component/render-effect [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} c]
    (when target
      (let [source* @source
            target* @target]
        (c/line c
                (start-point source* target*)
                (end-point source* target* maxrange)
                (if (in-range? source* target* maxrange)
                  [1 0 0 0.5]
                  [1 1 0 0.5]))))))
