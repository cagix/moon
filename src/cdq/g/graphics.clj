(ns cdq.g.graphics
  (:require [cdq.g :as g]
            [gdl.graphics :as graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.viewport :as viewport]
            [gdl.tiled :as tiled]))

; TODO I understand - this knows about graphics keys .... only here ....
; * line-of-sight - on-screen?

; => all 'gdl.graphics' -> frames-per-second, etc.
; => then make separate object !?

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- with-line-width [shape-drawer width draw-fn]
  (sd/with-line-width shape-drawer width draw-fn))

(defmulti draw! (fn [[k] _ctx]
                  k))

(defmethod draw! :draw/image [[_ {:keys [texture-region color] :as image} position]
                              {:keys [ctx/batch
                                      ctx/unit-scale]}]
  (batch/draw-texture-region! batch
                              texture-region
                              position
                              (unit-dimensions image unit-scale)
                              0 ; rotation
                              color))

(defmethod draw! :draw/rotated-centered [[_ {:keys [texture-region color] :as image} rotation [x y]]
                                         {:keys [ctx/batch
                                                 ctx/unit-scale]}]
  (let [[w h] (unit-dimensions image unit-scale)]
    (batch/draw-texture-region! batch
                                texture-region
                                [(- (float x) (/ (float w) 2))
                                 (- (float y) (/ (float h) 2))]
                                [w h]
                                rotation
                                color)))

(defmethod draw! :draw/centered [[_ image position] ctx]
  (draw! [:draw/rotated-centered image 0 position] ctx))

(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [ctx/default-font
                                     ctx/batch
                                     ctx/unit-scale]}]
  (graphics/draw-text! (or font default-font)
                       batch
                       {:scale (* (float unit-scale)
                                  (float (or scale 1)))
                        :x x
                        :y y
                        :text text
                        :h-align h-align
                        :up? up?}))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color] ctx]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw! [:draw/line [linex topy] [linex bottomy] color] ctx))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw! [:draw/line [leftx liney] [rightx liney] color] ctx))))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [ctx/shape-drawer]
                                         :as ctx}]
  (sd/with-line-width shape-drawer width
    (fn []
      (g/handle-draws! ctx draws))))

; doc for :draw/text:
;  "font, h-align, up? and scale are optional.
;  h-align one of: :center, :left, :right. Default :center.
;  up? renders the font over y, otherwise under.
;  scale will multiply the drawn text size with the scale."

(extend-type cdq.g.Game
  g/Graphics
  (handle-draws! [ctx draws]
    (doseq [component draws
            :when component]
      (draw! component ctx)))

  (draw-on-world-viewport! [{:keys [ctx/batch
                                    ctx/world-viewport
                                    ctx/world-unit-scale
                                    ctx/shape-drawer]
                             :as ctx} fns]
    (batch/draw-on-viewport! batch
                             world-viewport
                             (fn []
                               (sd/with-line-width shape-drawer world-unit-scale
                                 (fn []
                                   (doseq [f fns]
                                     (f (assoc ctx :ctx/unit-scale world-unit-scale))))))))

  (world-mouse-position [{:keys [ctx/world-viewport]}]
    (viewport/mouse-position world-viewport))

  (ui-mouse-position [{:keys [ctx/ui-viewport]}]
    (viewport/mouse-position ui-viewport))

  (update-viewports! [{:keys [ctx/ui-viewport
                              ctx/world-viewport]}]
    (viewport/update! ui-viewport)
    (viewport/update! world-viewport))

  (draw-tiled-map! [{:keys [ctx/tiled-map-renderer
                            ctx/world-viewport]}
                    tiled-map
                    color-setter]
    (tiled/draw! (tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport)))

  (camera-position [{:keys [ctx/world-viewport]}]
    (camera/position (:camera world-viewport)))

  (inc-zoom! [{:keys [ctx/world-viewport]} amount]
    (camera/inc-zoom! (:camera world-viewport) amount))

  (camera-frustum [{:keys [ctx/world-viewport]}]
    (camera/frustum (:camera world-viewport)))

  (visible-tiles [{:keys [ctx/world-viewport]}]
    (camera/visible-tiles (:camera world-viewport)))

  (set-camera-position! [{:keys [ctx/world-viewport]} position]
    (camera/set-position! (:camera world-viewport)
                          position))

  (camera-zoom [{:keys [ctx/world-viewport]}]
    (camera/zoom (:camera world-viewport)))

  (pixels->world-units [{:keys [ctx/world-unit-scale]} pixels]
    (* pixels world-unit-scale))
  )
