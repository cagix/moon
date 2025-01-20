(ns cdq.render.draw-on-world-view.entities
  (:require cdq.graphics
            [cdq.timer :as timer]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.graphics.batch :as batch]
            [cdq.graphics.text :as text]
            [cdq.line-of-sight :as los]
            [cdq.utils :refer [pretty-pst defsystem sort-by-order]]
            [cdq.val-max :as val-max]
            [cdq.world :refer [render-z-order
                               draw-body-rect
                               world-item?
                               item-place-position]]
            [cdq.entity :as entity]))

(defn- draw-skill-image [{:keys [cdq.graphics/shape-drawer] :as c} image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (sd/filled-circle shape-drawer center radius [1 1 1 0.125])
    (sd/sector shape-drawer
               center
               radius
               90 ; start-angle
               (* (float action-counter-ratio) 360) ; degree
               [1 1 1 0.5])
    (batch/draw-image c image [(- (float x) radius) y])))

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [{:keys [cdq.graphics/shape-drawer] :as c}
                   {:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (cdq.graphics/pixels->world-units c 5)
          border (cdq.graphics/pixels->world-units c borders-px)]
      (sd/filled-rectangle shape-drawer x y width height :black)
      (sd/filled-rectangle shape-drawer
                           (+ x border)
                           (+ y border)
                           (- (* width ratio) (* 2 border))
                           (- height          (* 2 border))
                           (hpbar-color ratio)))))

(defn draw-text-when-mouseover-and-text
  [{:keys [text]}
   {:keys [entity/mouseover?] :as entity}
   c]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (text/draw c
                 {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))

(defn draw-hpbar-when-mouseover-and-not-full[ _ entity c]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar c entity ratio))))

(defn draw-image-as-of-body [image entity c]
  (batch/draw-rotated-centered c
                               image
                               (or (:rotation-angle entity) 0)
                               (:position entity)))

(defn draw-line
  [{:keys [thick? end color]}
   entity
   {:keys [cdq.graphics/shape-drawer]}]
  (let [position (:position entity)]
    (if thick?
      (sd/with-line-width shape-drawer 4
        #(sd/line shape-drawer position end color))
      (sd/line shape-drawer position end color))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defn draw-faction-ellipse
  [_
   {:keys [entity/faction] :as entity}
   {:keys [cdq.context/player-eid
           cdq.graphics/shape-drawer] :as c}]
  (let [player @player-eid]
    (sd/with-line-width shape-drawer 3
      #(sd/ellipse shape-drawer
                   (:position entity)
                   (:half-width entity)
                   (:half-height entity)
                   (cond (= faction (entity/enemy player))
                         enemy-color
                         (= faction (:entity/faction player))
                         friendly-color
                         :else
                         neutral-color)))))

(defsystem render-effect)
(defmethod render-effect :default [_ _effect-ctx context])

(defn- render-active-effect [context effect-ctx effect]
  (run! #(render-effect % effect-ctx context) effect))

(defn draw-skill-image-and-active-effect
  [{:keys [skill effect-ctx counter]}
   entity
   {:keys [cdq.context/elapsed-time] :as c}]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image c
                      image
                      entity
                      (:position entity)
                      (timer/ratio counter elapsed-time))
    (render-active-effect c
                          effect-ctx
                          ; !! FIXME !!
                          ; (update-effect-ctx c effect-ctx)
                          ; - render does not need to update .. update inside active-skill
                          effects)))

(defn draw-zzzz [_ entity c]
  (let [[x y] (:position entity)]
    (text/draw c
               {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))

(defn draw-world-item-if-exists [{:keys [item]} entity c]
  (when (world-item? c)
    (batch/draw-centered c
                         (:entity/image item)
                         (item-place-position c entity))))


(defmethod entity/draw-gui-view :player-item-on-cursor
  [[_ {:keys [eid]}] {:keys [cdq.graphics/ui-viewport] :as c}]
  (when (not (world-item? c))
    (batch/draw-centered c
                         (:entity/image (:entity/item-on-cursor @eid))
                         (cdq.graphics/mouse-position ui-viewport))))

(defn draw-stunned-circle [_ entity {:keys [cdq.graphics/shape-drawer]}]
  (sd/circle shape-drawer (:position entity) 0.5 [1 1 1 0.6]))

(defn draw-text [{:keys [text]} entity c]
  (let [[x y] (:position entity)]
    (text/draw c
               {:text text
                :x x
                :y (+ y
                      (:half-height entity)
                      (cdq.graphics/pixels->world-units c 5))
                :scale 2
                :up? true})))

; TODO draw opacity as of counter ratio?
(defn draw-filled-circle-grey [_ entity {:keys [cdq.graphics/shape-drawer]}]
  (sd/filled-circle shape-drawer (:position entity) 0.5 [0.5 0.5 0.5 0.4]))

(defmethod render-effect :effects/target-all
  [_ {:keys [effect/source]} {:keys [cdq.graphics/shape-drawer] :as c}]
  (let [source* @source]
    (doseq [target* (map deref (los/creatures-in-los-of-player c))]
      (sd/line shape-drawer
               (:position source*) #_(start-point source* target*)
               (:position target*)
               [1 0 0 0.5]))))

(defmethod render-effect :effects/target-entity
  [[_ {:keys [maxrange]}]
   {:keys [effect/source effect/target]}
   {:keys [cdq.graphics/shape-drawer]}]
  (when target
    (let [source* @source
          target* @target]
      (sd/line shape-drawer
               (entity/start-point source* target*)
               (entity/end-point source* target* maxrange)
               (if (entity/in-range? source* target* maxrange)
                 [1 0 0 0.5]
                 [1 1 0 0.5])))))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn render-entities
  [{:keys [below
           default
           above
           info]}
   {:keys [cdq.context/player-eid
           cdq.graphics/shape-drawer
           cdq.game/active-entities] :as c}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [below
                    default
                    above
                    info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (los/exists? c player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect shape-drawer entity (if (:collides? entity) :white :gray)))
       (doseq [[k v] entity
               :let [f (get system k)]
               :when f]
         (assert (resolve f) (str "k: " k, ", f: " f))
         (@(resolve f) v entity c))
       (catch Throwable t
         (draw-body-rect shape-drawer entity :red)
         (pretty-pst t))))))
