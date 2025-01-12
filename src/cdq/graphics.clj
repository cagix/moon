(ns cdq.graphics
  (:require [gdl.context :as c]
            [cdq.context.timer :as timer]
            [cdq.error :refer [pretty-pst]]
            [clojure.graphics.camera :as cam]
            [clojure.graphics.shape-drawer :as sd]
            [cdq.math.shapes :refer [circle->outer-rectangle]]
            [gdl.utils :refer [defsystem sort-by-order]]
            [cdq.val-max :as val-max]
            [cdq.context :refer [grid-cell
                                 active-entities
                                 render-z-order
                                 line-of-sight?
                                 draw-body-rect
                                 creatures-in-los-of-player
                                 world-item?
                                 item-place-position
                                 circle->cells]]
            [cdq.entity :as entity]))

(defn- geom-test [{:keys [gdl.graphics/shape-drawer] :as c}]
  (let [position (c/world-mouse-position c)
        radius 0.8
        circle {:position position :radius radius}]
    (sd/circle shape-drawer position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (circle->cells c circle))]
      (sd/rectangle shape-drawer x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (sd/rectangle shape-drawer x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [{:keys [gdl.graphics/shape-drawer] :as c}]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (c/world-mouse-position c))
          cell (grid-cell c [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (sd/rectangle shape-drawer x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5]))))))

(defn render-after-entities [c]
  #_(geom-test c)
  (highlight-mouseover-tile c))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn render-before-entities [{:keys [gdl.graphics/world-viewport
                                      gdl.graphics/shape-drawer
                                      cdq.context/factions-iterations]
                               :as c}]
  (let [sd shape-drawer
        cam (:camera world-viewport)
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (sd/grid sd
               (int left-x) (int bottom-y)
               (inc (int (:width  world-viewport)))
               (+ 2 (int (:height world-viewport)))
               1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (grid-cell c [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (sd/filled-rectangle sd x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (sd/filled-rectangle sd x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (sd/filled-rectangle sd x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- draw-skill-image [{:keys [gdl.graphics/shape-drawer] :as c} image entity [x y] action-counter-ratio]
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
    (c/draw-image c image [(- (float x) radius) y])))

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

(defn- draw-hpbar [{:keys [gdl.graphics/shape-drawer] :as c}
                   {:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (c/pixels->world-units c 5)
          border (c/pixels->world-units c borders-px)]
      (sd/filled-rectangle shape-drawer x y width height :black)
      (sd/filled-rectangle shape-drawer
                           (+ x border)
                           (+ y border)
                           (- (* width ratio) (* 2 border))
                           (- height          (* 2 border))
                           (hpbar-color ratio)))))

(defsystem below)
(defmethod below :default [_ entity c])

(defsystem default)
(defmethod default :default [_ entity c])

(defsystem above)
(defmethod above :default [_ entity c])

(defsystem info)
(defmethod info :default [_ entity c])

(defmethod default :entity/clickable
  [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity} c]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))

(defmethod info :entity/hp
  [ _ entity c]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar c entity ratio))))

(defmethod default :entity/image
  [[_ image] entity c]
  (c/draw-rotated-centered c
                           image
                           (or (:rotation-angle entity) 0)
                           (:position entity)))

(defmethod default :entity/line-render
  [[_ {:keys [thick? end color]}] entity {:keys [gdl.graphics/shape-drawer]}]
  (let [position (:position entity)]
    (if thick?
      (sd/with-line-width shape-drawer 4
        #(sd/line shape-drawer position end color))
      (sd/line shape-drawer position end color))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod below :entity/mouseover?
  [_
   {:keys [entity/faction] :as entity}
   {:keys [cdq.context/player-eid
           gdl.graphics/shape-drawer] :as c}]
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

(defmethod info :active-skill
  [[_ {:keys [skill effect-ctx counter]}] entity c]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image c
                      image
                      entity
                      (:position entity)
                      (timer/ratio c counter))
    (render-active-effect c
                          effect-ctx
                          ; !! FIXME !!
                          ; (update-effect-ctx c effect-ctx)
                          ; - render does not need to update .. update inside active-skill
                          effects)))

(defmethod above :npc-sleeping
  [_ entity c]
  (let [[x y] (:position entity)]
    (c/draw-text c
                 {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true})))

(defmethod below :player-item-on-cursor
  [[_ {:keys [item]}] entity c]
  (when (world-item? c)
    (c/draw-centered c
                     (:entity/image item)
                     (item-place-position c entity))))


(defmethod entity/draw-gui-view :player-item-on-cursor
  [[_ {:keys [eid]}] c]
  (when (not (world-item? c))
    (c/draw-centered c
                     (:entity/image (:entity/item-on-cursor @eid))
                     (c/mouse-position c))))

(defmethod below :stunned
  [_ entity {:keys [gdl.graphics/shape-drawer]}]
  (sd/circle shape-drawer (:position entity) 0.5 [1 1 1 0.6]))

(defmethod above :entity/string-effect
  [[_ {:keys [text]}] entity c]
  (let [[x y] (:position entity)]
    (c/draw-text c
                 {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (c/pixels->world-units c 5))
                  :scale 2
                  :up? true})))

; TODO draw opacity as of counter ratio?
(defmethod above :entity/temp-modifier
  [_ entity {:keys [gdl.graphics/shape-drawer]}]
  (sd/filled-circle shape-drawer (:position entity) 0.5 [0.5 0.5 0.5 0.4]))

(defmethod render-effect :effects/target-all
  [_ {:keys [effect/source]} {:keys [gdl.graphics/shape-drawer] :as c}]
  (let [source* @source]
    (doseq [target* (map deref (creatures-in-los-of-player c))]
      (sd/line shape-drawer
               (:position source*) #_(start-point source* target*)
               (:position target*)
               [1 0 0 0.5]))))

(defmethod render-effect :effects/target-entity
  [[_ {:keys [maxrange]}]
   {:keys [effect/source effect/target]}
   {:keys [gdl.graphics/shape-drawer]}]
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
  "Draws all active entities, sorted by the `:z-order` and with the render systems `below`, `default`, `above`, `info` for each z-order if the entity is in line-of-sight? to the player entity or is an `:z-order/effect`.

  Optionally for debug purposes body rectangles can be drown which show white for collidings and gray for non colliding entities.

  If an error is thrown during rendering, the entity body drawn with a red rectangle and the error is pretty printed to the console."
  [{:keys [cdq.context/player-eid
           gdl.graphics/shape-drawer] :as c}]
  (let [entities (map deref (active-entities c))
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [below default above info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? c player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect shape-drawer entity (if (:collides? entity) :white :gray)))
       (run! #(system % entity c) entity)
       (catch Throwable t
         (draw-body-rect shape-drawer entity :red)
         (pretty-pst t))))))
