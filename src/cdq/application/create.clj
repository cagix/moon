; * all gdl dependencies here (actor ?) -> frames-per-second ...
; => gdl still required ?
; * private functions out -> sprite, assets, etc.
; ?
; * dispose/schema sees this too ...
(ns cdq.application.create
  (:require [gdl.assets :as assets]
            [cdq.db :as db]
            [cdq.g :as g]
            [cdq.utils :as utils :refer [mapvals]]
            [clojure.string :as str]
            [gdl.graphics :as graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [cdq.utils.files :as files]))

(defn- create-assets [{:keys [folder
                              asset-type-extensions]}]
  (assets/create
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (files/recursively-search folder extensions))]
     [file asset-type])))

(defn- create-app-state [config]
  (run! require (:requires config))
  (ui/load! (:ui config))
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))
        ui-viewport (graphics/ui-viewport (:ui-viewport config))
        stage (ui/stage (:java-object ui-viewport)
                        (:java-object batch))]
    (input/set-processor! stage)
    (cdq.g/map->Game
     {:ctx/config config
      :ctx/db (db/create (:db config))
      :ctx/assets (create-assets (:assets config))
      :ctx/batch batch
      :ctx/unit-scale 1
      :ctx/world-unit-scale world-unit-scale
      :ctx/shape-drawer-texture shape-drawer-texture
      :ctx/shape-drawer (graphics/shape-drawer batch (graphics/texture-region shape-drawer-texture 1 0 1 1))
      :ctx/cursors (mapvals
                    (fn [[file [hotspot-x hotspot-y]]]
                      (graphics/cursor (format (:cursor-path-format config) file)
                                       hotspot-x
                                       hotspot-y))
                    (:cursors config))
      :ctx/default-font (graphics/truetype-font (:default-font config))
      :ctx/world-viewport (graphics/world-viewport world-unit-scale (:world-viewport config))
      :ctx/ui-viewport ui-viewport
      :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                         (tiled/renderer tiled-map
                                                         world-unit-scale
                                                         (:java-object batch))))
      :ctx/stage stage})))

(defn do! [config]
  (g/reset-game-state! (create-app-state config)))

(extend-type cdq.g.Game
  g/Config
  (config [{:keys [ctx/config]} key]
    (get config key)))

(extend-type cdq.g.Game
  g/Database
  (get-raw [{:keys [ctx/db]} property-id]
    (db/get-raw db property-id))

  (build [{:keys [ctx/db] :as ctx} property-id]
    (db/build db property-id ctx))

  (build-all [{:keys [ctx/db] :as ctx} property-type]
    (db/build-all db property-type ctx))

  (property-types [{:keys [ctx/db]}]
    (filter #(= "properties" (namespace %)) (keys (:schemas db))))

  (schemas [{:keys [ctx/db]}]
    (:schemas db))

  (update-property! [{:keys [ctx/db] :as ctx}
                     property]
    (let [new-db (db/update db property)]
      (db/save! new-db)
      (assoc ctx :ctx/db new-db)))

  (delete-property! [{:keys [ctx/db] :as ctx}
                     property-id]
    (let [new-db (db/delete db property-id)]
      (db/save! new-db)
      (assoc ctx :ctx/db new-db))))

(extend-type cdq.g.Game
  g/Textures
  (texture [{:keys [ctx/assets]} path]
    (assets path))

  (all-textures [{:keys [ctx/assets]}]
    (assets/all-of-type assets :texture))

  g/Sounds
  (sound [{:keys [ctx/assets]} path]
    (assets path))

  (all-sounds [{:keys [ctx/assets]}]
    (assets/all-of-type assets :sound)))

(extend-type cdq.g.Game
  g/Input
  (button-just-pressed? [_ button]
    (input/button-just-pressed? button))

  (key-pressed? [_ key]
    (input/key-pressed? key))

  (key-just-pressed? [_ key]
    (input/key-just-pressed? key)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} scale world-unit-scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (graphics/dimensions texture-region)
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

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

  (world-viewport-width [{:keys [ctx/world-viewport]}]
    (:width world-viewport))

  (world-viewport-height [{:keys [ctx/world-viewport]}]
    (:height world-viewport))

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

  (sprite [{:keys [ctx/world-unit-scale] :as ctx}
           texture-path]
    (sprite* (graphics/texture-region (g/texture ctx texture-path))
             world-unit-scale))

  (sub-sprite [{:keys [ctx/world-unit-scale]}
               sprite
               [x y w h]]
    (sprite* (graphics/sub-region (:texture-region sprite) x y w h)
             world-unit-scale))

  (sprite-sheet [{:keys [ctx/world-unit-scale] :as ctx}
                 texture-path
                 tilew
                 tileh]
    {:image (sprite* (graphics/texture-region (g/texture ctx texture-path))
                     world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [ctx
                         {:keys [image tilew tileh]}
                         [x y]]
    (g/sub-sprite ctx
                  image
                  [(* x tilew) (* y tileh) tilew tileh]))

  (set-cursor! [{:keys [ctx/cursors]} cursor-key]
    (graphics/set-cursor! (utils/safe-get cursors cursor-key))))
