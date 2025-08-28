(ns cdq.render.draw-on-world-viewport
  (:require [cdq.app :as app]
            [cdq.animation :as animation]
            [cdq.c :as c]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.state.player-item-on-cursor]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.grid :as grid]
            [cdq.math.geom :as geom]
            [cdq.timer :as timer]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [cdq.w :as w]))

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn- draw-tile-grid* [world-viewport]
  (let [[left-x _right-x bottom-y _top-y] (camera/frustum (:viewport/camera world-viewport))]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (:viewport/width world-viewport)))
      (+ 2 (int (:viewport/height world-viewport)))
      1
      1
      [1 1 1 0.8]]]))

(defn- draw-tile-grid [{:keys [ctx/graphics] :as ctx}]
  (when show-tile-grid?
    (graphics/handle-draws! graphics (draw-tile-grid* (:world-viewport graphics)))))

(defn- draw-cell-debug* [{:keys [ctx/world
                                 ctx/graphics]}]
  (let [grid (:world/grid world)]
    (apply concat
           (for [[x y] (camera/visible-tiles (:viewport/camera (:world-viewport graphics)))
                 :let [cell (grid/cell grid [x y])]
                 :when cell
                 :let [cell* @cell]]
             [(when (and show-cell-entities? (seq (:entities cell*)))
                [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
              (when (and show-cell-occupied? (seq (:occupied cell*)))
                [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
              (when-let [faction show-potential-field-colors?]
                (let [{:keys [distance]} (faction cell*)]
                  (when distance
                    (let [ratio (/ distance ((:world/factions-iterations world) faction))]
                      [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))]))))

(defn- draw-cell-debug [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (draw-cell-debug* ctx)))

(def ^:private skill-image-radius-world-units
  (let [tile-size 48
        image-width 32]
    (/ (/ image-width tile-size) 2)))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(def ^:private mouseover-ellipse-width 5)

(def stunned-circle-width 0.5)
(def stunned-circle-color [1 1 1 0.6])

(defn draw-item-on-cursor-state [{:keys [item]} entity ctx]
  (when (cdq.entity.state.player-item-on-cursor/world-item? ctx)
    [[:draw/centered
      (:entity/image item)
      (cdq.entity.state.player-item-on-cursor/item-place-position ctx entity)]]))

(defn draw-mouseover-highlighting [_ entity {:keys [ctx/world]}]
  (let [player @(:world/player-eid world)
        faction (entity/faction entity)]
    [[:draw/with-line-width mouseover-ellipse-width
      [[:draw/ellipse
        (entity/position entity)
        (/ (:body/width  (:entity/body entity)) 2)
        (/ (:body/height (:entity/body entity)) 2)
        (cond (= faction (entity/enemy player))
              enemy-color
              (= faction (entity/faction player))
              friendly-color
              :else
              neutral-color)]]]]))

(defn draw-stunned-state [_ entity _ctx]
  [[:draw/circle
    (entity/position entity)
    stunned-circle-width
    stunned-circle-color]])

(defn draw-clickable-mouseover-text [{:keys [text]} {:keys [entity/mouseover?] :as entity} _ctx]
  (when (and mouseover? text)
    (let [[x y] (entity/position entity)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y (/ (:body/height (:entity/body entity)) 2))
                    :up? true}]])))

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [radius skill-image-radius-world-units
        y (+ (float y)
             (float (/ (:body/height (:entity/body entity)) 2))
             (float 0.15))
        center [x (+ y radius)]]
    [[:draw/filled-circle center radius [1 1 1 0.125]]
     [:draw/sector
      center
      radius
      90 ; start-angle
      (* (float action-counter-ratio) 360) ; degree
      [1 1 1 0.5]]
     [:draw/image image [(- (float x) radius) y]]]))

(defn- render-active-effect [ctx effect-ctx effect]
  (mapcat #(effect/render % effect-ctx ctx) effect))

(defn draw-active-skill [{:keys [skill effect-ctx counter]}
                         entity
                         {:keys [ctx/world] :as ctx}]
  (let [{:keys [entity/image skill/effects]} skill]
    (concat (draw-skill-image image
                              entity
                              (entity/position entity)
                              (timer/ratio (:world/elapsed-time world) counter))
            (render-active-effect ctx
                                  effect-ctx ; TODO !!!
                                  ; !! FIXME !!
                                  ; (update-effect-ctx effect-ctx)
                                  ; - render does not need to update .. update inside active-skill
                                  effects))))

(defn draw-centered-rotated-image [image entity _ctx]
  [[:draw/rotated-centered
    image
    (or (:body/rotation-angle (:entity/body entity)) 0)
    (entity/position entity)]])

(defn call-render-image [animation entity ctx]
  (draw-centered-rotated-image (animation/current-frame animation)
                               entity
                               ctx))

(defn draw-line-entity [{:keys [thick? end color]} entity _ctx]
  (let [position (entity/position entity)]
    (if thick?
      [[:draw/with-line-width 4 [[:draw/line position end color]]]]
      [[:draw/line position end color]])))

(defn draw-sleeping-state [_ entity _ctx]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text "zzz"
                  :x x
                  :y (+ y (/ (:body/height (:entity/body entity)) 2))
                  :up? true}]]))

; TODO draw opacity as of counter ratio?
(defn draw-temp-modifiers [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])

(defn- draw-text-over-entity [{:keys [text]} entity {:keys [ctx/graphics]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (/ (:body/height (:entity/body entity)) 2)
                        (* 5 (:world-unit-scale graphics)))
                  :scale 2
                  :up? true}]]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0 1]
   :darkgreen [0 0.5 0 1]
   :yellow    [0.5 0.5 0 1]
   :red       [0.5 0 0 1]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [world-unit-scale {:keys [body/position body/width body/height]} ratio]
  (let [[x y] position]
    (let [x (- x (/ width  2))
          y (+ y (/ height 2))
          height (* 5          world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height :black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defn draw-stats [_ entity {:keys [ctx/graphics]}]
  (let [ratio (val-max/ratio (entity/hitpoints entity))] ; <- use stats directly?
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar (:world-unit-scale graphics)
                  (:entity/body entity)
                  ratio))))

(def render-below {:entity/mouseover? draw-mouseover-highlighting
                   :stunned draw-stunned-state
                   :player-item-on-cursor draw-item-on-cursor-state})

(def render-default {:entity/clickable draw-clickable-mouseover-text
                     :entity/animation call-render-image
                     :entity/image draw-centered-rotated-image
                     :entity/line-render draw-line-entity})

(def render-above {:npc-sleeping draw-sleeping-state
                   :entity/temp-modifier draw-temp-modifiers
                   :entity/string-effect draw-text-over-entity})

(def render-info {:creature/stats draw-stats
                  :active-skill draw-active-skill})

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-entity [{:keys [ctx/app ctx/graphics] :as ctx} entity render-layer]
  (try
   (when show-body-bounds?
     (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) (if (:body/collides? (:entity/body entity)) :white :gray))))
   (doseq [[k v] entity
           :let [draw-fn (get render-layer k)]
           :when draw-fn]
     (graphics/handle-draws! graphics (draw-fn v entity ctx)))
   (catch Throwable t
     (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) :red))
     (app/pretty-pst app t))))

(defn- render-entities
  [{:keys [ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (w/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer [render-below
                          render-default
                          render-above
                          render-info]
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

(defn- geom-test* [{:keys [ctx/world] :as ctx}]
  (let [grid (:world/grid world)
        position (c/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn geom-test [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (geom-test* ctx)))

(defn- highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world] :as ctx}]
  (graphics/handle-draws! graphics
                          (let [[x y] (mapv int (c/world-mouse-position ctx))
                                cell (grid/cell (:world/grid world) [x y])]
                            (when (and cell (#{:air :none} (:movement @cell)))
                              [[:draw/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5])]]))))

(defn do! [{:keys [ctx/graphics] :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [f [draw-tile-grid
                                                 draw-cell-debug
                                                 render-entities
                                                 ; geom-test
                                                 highlight-mouseover-tile]]
                                        (f ctx))))
  ctx)
