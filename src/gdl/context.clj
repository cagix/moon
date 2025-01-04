(ns gdl.context
  (:require [clojure.gdx :as gdx :refer [play clamp degree->radians white set-projection-matrix begin end set-color draw unproject input-x input-y key-pressed?]]
            [clojure.gdx.assets :as assets]
            [clojure.gdx.file-handle :as fh]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.g2d.bitmap-font :as font]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.math.vector2 :as v]
            [clojure.gdx.vis-ui :as vis-ui]
            [clojure.string :as str]
            [gdl.utils :refer [defsystem defcomponent safe-get with-err-str mapvals read-edn-resource]]
            [gdl.db :as db]
            [gdl.error :refer [pretty-pst]]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.sprite :as sprite]
            [gdl.graphics.camera :as cam]
            [gdl.malli :as m]
            [gdl.schema :as schema]
            [clojure.gdx.tiled :as tiled]
            [gdl.ui :as ui]
            [clojure.gdx.scene2d.group :as group])
  (:import (com.kotcrab.vis.ui.widget Tooltip)
           (gdl OrthogonalTiledMapRenderer ColorSetter)))

(defsystem create)
(defmethod create :default [[_ v] _context]
  v)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem resize)
(defmethod resize :default [_ width height])

(defn- safe-create-into [context components]
  (reduce (fn [context [k v]]
            (assert (not (contains? context k)))
            (assoc context k (create [k v] context)))
          context
          components))

(defn- reduce-transact [value fns]
  (reduce (fn [value f]
            (f value))
          value
          fns))

(def state (atom nil))

(comment
 (clojure.pprint/pprint (sort (keys @state)))
 )

(defn- load-tx [tx-sym]
  (require (symbol (namespace tx-sym)))
  (resolve tx-sym))

(defn start
  "A transaction is a `(fn [context] context)`, which can emit also side-effects or return a new context.

  Given a string to a `java.io/resource` or a map of `{:keys [config context transactions]}`."
  [app-config]
  (let [{:keys [config context transactions]} (if (string? app-config)
                                                (read-edn-resource app-config)
                                                app-config)
        txs (doall (map load-tx transactions))]
    (lwjgl/start config
                 (reify lwjgl/Application
                   (create [_]
                     (reset! state (safe-create-into (gdx/context) context)))

                   (dispose [_]
                     (run! dispose @state))

                   (render [_]
                     (swap! state reduce-transact txs))

                   (resize [_ width height]
                     (run! #(resize % width height) @state))))))

(defn -main
  "Calls [start] with `\"gdl.app.edn\"`."
  []
  (start "gdl.app.edn"))

(defn- load-all [manager assets]
  (doseq [[file asset-type] assets]
    (assets/load manager file asset-type))
  (assets/finish-loading manager))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list folder)
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn- sd-texture []
  (let [pixmap (doto (gdx/pixmap 1 1 pixmap/format-RGBA8888)
                 (pixmap/set-color gdx/white)
                 (gdx/draw-pixel 0 0))
        texture (gdx/texture pixmap)]
    (gdx/dispose pixmap)
    texture))

(defcomponent :gdl.context/assets
  (create [[_ folder] context]
    (doto (gdx/asset-manager)
      (load-all (for [[asset-type exts] {:sound   #{"wav"}
                                         :texture #{"png" "bmp"}}
                      file (map #(str/replace-first % folder "")
                                (recursively-search (gdx/internal-file context folder)
                                                    exts))]
                  [file asset-type]))))

  (dispose [[_ asset-manager]]
    (gdx/dispose asset-manager)))

(defcomponent :gdl.context/batch
  (create [_ _context]
    (gdx/sprite-batch))

  (dispose [[_ batch]]
    (gdx/dispose batch)))

(defcomponent :gdl.context/cursors
  (create [[_ cursors] c]
    (mapvals (fn [[file [hotspot-x hotspot-y]]]
               (let [pixmap (gdx/pixmap (gdx/internal-file c (str "cursors/" file ".png")))
                     cursor (gdx/cursor c pixmap hotspot-x hotspot-y)]
                 (gdx/dispose pixmap)
                 cursor))
             cursors))

  (dispose [[_ cursors]]
    (run! gdx/dispose (vals cursors))))

(defcomponent :gdl.context/db
  (create [[_ config] _context]
    (db/create config)))

(defcomponent :gdl.context/default-font
  (create [[_ config] context]
    (freetype/generate-font (update config :file #(gdx/internal-file context %))))

  (dispose [[_ font]]
    (gdx/dispose font)))

(defcomponent :gdl.context/shape-drawer
  (create [_ {:keys [gdl.context/batch]}]
    (assert batch)
    (sd/create batch (gdx/texture-region (sd-texture) 1 0 1 1)))

  (dispose [[_ sd]]
    #_(gdx/dispose sd))
  ; TODO this will break ... proxy with extra-data -> get texture through sd ...
  ; => shape-drawer-texture as separate component?!
  ; that would work
  )

(defcomponent :gdl.context/stage
  (create [_ {:keys [gdl.context/viewport
                     gdl.context/batch] :as c}]
    (let [stage (ui/stage viewport batch nil)]
      (gdx/set-input-processor c stage)
      stage))

  (dispose [[_ stage]]
    (gdx/dispose stage)))

(defcomponent :gdl.context/tiled-map-renderer
  (create [_ {:keys [gdl.context/world-unit-scale
                     gdl.context/batch]}]
    (assert world-unit-scale)
    (assert batch)
    (memoize (fn [tiled-map]
               (OrthogonalTiledMapRenderer. tiled-map
                                            (float world-unit-scale)
                                            batch)))))

(defcomponent :gdl.context/ui
  (create [[_ skin-scale] _c]
    ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
    ; => actually there is a deeper issue at play
    ; we need to dispose ALL resources which were loaded already ...
    (when (vis-ui/loaded?)
      (vis-ui/dispose))
    (vis-ui/load skin-scale)
    (-> (vis-ui/skin)
        (.getFont "default-font")
        .getData
        .markupEnabled
        (set! true))
    ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
    ;Controls whether to fade out tooltip when mouse was moved. (default false)
    ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
    (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))

  (dispose [_]
    (vis-ui/dispose)))

(defcomponent :gdl.context/viewport
  (create [[_ {:keys [width height]}] _c]
    (gdx/fit-viewport width height (gdx/orthographic-camera)))

  (resize [[_ viewport] w h]
    (gdx/resize viewport w h :center-camera? true)))

(defcomponent :gdl.context/world-unit-scale
  (create [[_ tile-size] _c]
    (float (/ tile-size))))

(defcomponent :gdl.context/world-viewport
  (create [[_ {:keys [width height]}] {:keys [gdl.context/world-unit-scale]}]
    (assert world-unit-scale)
    (let [camera (gdx/orthographic-camera)
          world-width  (* width  world-unit-scale)
          world-height (* height world-unit-scale)]
      (camera/set-to-ortho camera world-width world-height :y-down? false)
      (gdx/fit-viewport world-width world-height camera)))

  (resize [[_ viewport] w h]
    (gdx/resize viewport w h :center-camera? false)))

(defn get-sound [{::keys [assets]} sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))

(defn play-sound [c sound-name]
  (play (get-sound c sound-name)))

(defn- texture-region [{::keys [assets]} path]
  (gdx/texture-region (assets path)))

(defn sprite [{::keys [world-unit-scale] :as c} path]
  (sprite/create world-unit-scale
                 (texture-region c path)))

(defn sub-sprite [{::keys [world-unit-scale]} sprite xywh]
  (sprite/sub world-unit-scale
              sprite
              xywh))

(defn sprite-sheet [{::keys [world-unit-scale] :as c} path tilew tileh]
  (sprite/sheet world-unit-scale
                (texture-region c path)
                tilew
                tileh))

(defn from-sprite-sheet [{::keys [world-unit-scale]} sprite-sheet xy]
  (sprite/from-sheet world-unit-scale
                     sprite-sheet
                     xy))

(defn set-cursor [{::keys [cursors] :as c}
                  cursor-key]
  (gdx/set-cursor c (safe-get cursors cursor-key)))

(defn- draw-tiled-map* [^OrthogonalTiledMapRenderer this tiled-map color-setter camera]
  (.setColorSetter this (reify ColorSetter
                          (apply [_ color x y]
                            (color-setter color x y))))
  (.setView this camera)
  (->> tiled-map
       tiled/layers
       (filter tiled/visible?)
       (map (partial tiled/layer-index tiled-map))
       int-array
       (.render this)))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [{::keys [world-viewport tiled-map-renderer]} tiled-map color-setter]
  (draw-tiled-map* (tiled-map-renderer tiled-map)
                   tiled-map
                   color-setter
                   (:camera world-viewport)))

(defn- munge-color [c]
  (cond (= com.badlogic.gdx.graphics.Color (class c)) c
        (keyword? c) (interop/k->color c)
        (vector? c) (apply gdx/color c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn- sd-color [shape-drawer color]
  (sd/set-color shape-drawer (munge-color color)))

(defn ellipse [{::keys [shape-drawer]} [x y] radius-x radius-y color]
  (sd-color shape-drawer color)
  (sd/ellipse shape-drawer x y radius-x radius-y))

(defn filled-ellipse [{::keys [shape-drawer]} [x y] radius-x radius-y color]
  (sd-color shape-drawer color)
  (sd/filled-ellipse shape-drawer x y radius-x radius-y))

(defn circle [{::keys [shape-drawer]} [x y] radius color]
  (sd-color shape-drawer color)
  (sd/circle shape-drawer x y radius))

(defn filled-circle [{::keys [shape-drawer]} [x y] radius color]
  (sd-color shape-drawer color)
  (sd/filled-circle shape-drawer x y radius))

(defn arc [{::keys [shape-drawer]} [center-x center-y] radius start-angle degree color]
  (sd-color shape-drawer color)
  (sd/arc shape-drawer center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn sector [{::keys [shape-drawer]} [center-x center-y] radius start-angle degree color]
  (sd-color shape-drawer color)
  (sd/sector shape-drawer center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn rectangle [{::keys [shape-drawer]} x y w h color]
  (sd-color shape-drawer color)
  (sd/rectangle shape-drawer x y w h))

(defn filled-rectangle [{::keys [shape-drawer]} x y w h color]
  (sd-color shape-drawer color)
  (sd/filled-rectangle shape-drawer x y w h))

(defn line [{::keys [shape-drawer]} [sx sy] [ex ey] color]
  (sd-color shape-drawer color)
  (sd/line shape-drawer sx sy ex ey))

(defn grid [{::keys [shape-drawer]} leftx bottomy gridw gridh cellw cellh color]
  (sd-color shape-drawer color)
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line [linex topy] [linex bottomy]))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line [leftx liney] [rightx liney]))))

(defn with-line-width [{::keys [shape-drawer]} width draw-fn]
  (let [old-line-width (sd/default-line-width shape-drawer)]
    (sd/set-default-line-width shape-drawer (* width old-line-width))
    (draw-fn)
    (sd/set-default-line-width shape-drawer old-line-width)))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (font/line-height font))))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{::keys [default-font batch unit-scale]}
   {:keys [font x y text h-align up? scale]}]
  (let [font (or font default-font)
        data (font/data font)
        old-scale (float (font/scale-x data))]
    (font/set-scale data (* old-scale
                            (float unit-scale)
                            (float (or scale 1))))
    (font/draw :font font
               :batch batch
               :text text
               :x x
               :y (+ y (if up? (text-height font text) 0))
               :align (interop/k->align (or h-align :center)))
    (font/set-scale data old-scale)))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- draw-texture-region [batch texture-region [x y] [w h] rotation color]
  (if color (set-color batch color))
  (draw batch
        texture-region
        :x x
        :y y
        :origin-x (/ (float w) 2) ; rotation origin
        :origin-y (/ (float h) 2)
        :width w
        :height h
        :scale-x 1
        :scale-y 1
        :rotation rotation)
  (if color (set-color batch white)))

(defn draw-image
  [{::keys [batch unit-scale]} {:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image unit-scale)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{::keys [batch unit-scale]} {:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image unit-scale)]
    (draw-texture-region batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn draw-centered [c image position]
  (draw-rotated-centered c image 0 position))

(defn- draw-on-viewport [batch viewport draw-fn]
  (set-color batch white) ; fix scene2d.ui.tooltip flickering
  (set-projection-matrix batch (camera/combined (:camera viewport)))
  (begin batch)
  (draw-fn)
  (end batch))

(defn draw-with [{::keys [batch] :as c} viewport unit-scale draw-fn]
  (draw-on-viewport batch
                    viewport
                    #(with-line-width c unit-scale
                       (fn []
                         (draw-fn (assoc c ::unit-scale unit-scale))))))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [c viewport]
  (let [mouse-x (clamp (input-x c)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (input-y c)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (unproject viewport mouse-x mouse-y)))

(defn mouse-position [{::keys [viewport] :as c}]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position c viewport)))

(defn world-mouse-position [{::keys [world-viewport] :as c}]
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position c world-viewport))

(defn pixels->world-units [{::keys [world-unit-scale]} pixels]
  (* (int pixels) world-unit-scale))

(defn draw-on-world-view [{::keys [world-viewport world-unit-scale] :as c} render-fn]
  (draw-with c world-viewport world-unit-scale render-fn))

(def stage ::stage)

(defn build [{::keys [db] :as c} id]
  (db/build db id c))

(defn build-all [{::keys [db] :as c} property-type]
  (db/build-all db property-type c))

(defn add-actor [{::keys [stage]} actor]
  (stage/add-actor stage actor))

(defn reset-stage [{::keys [stage]} new-actors]
  (stage/clear stage)
  (run! #(stage/add-actor stage %) new-actors))

(defn mouse-on-actor? [{::keys [stage] :as c}]
  (let [[x y] (mouse-position c)]
    (stage/hit stage x y true)))

(defn error-window [c throwable]
  (pretty-pst throwable)
  (add-actor c
   (ui/window {:title "Error"
               :rows [[(ui/label (binding [*print-level* 3]
                                   (with-err-str
                                     (clojure.repl/pst throwable))))]]
               :modal? true
               :close-button? true
               :close-on-escape? true
               :center? true
               :pack? true})))

; TODO do we care in here about malli-form ?! - where used? - hide inside 'schemas' ? or schemas/validation

(defmethod schema/malli-form :s/val-max [_ _schemas] m/val-max-schema)
(defmethod schema/malli-form :s/number  [_ _schemas] m/number-schema)
(defmethod schema/malli-form :s/nat-int [_ _schemas] m/nat-int-schema)
(defmethod schema/malli-form :s/int     [_ _schemas] m/int-schema)
(defmethod schema/malli-form :s/pos     [_ _schemas] m/pos-schema)
(defmethod schema/malli-form :s/pos-int [_ _schemas] m/pos-int-schema)

(defcomponent :s/sound
  (schema/malli-form [_ _schemas]
    m/string-schema)

  (db/edn->value [_ sound-name _db c]
    (get-sound c sound-name)))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (from-sprite-sheet c
                         (sprite-sheet c file tilew tileh)
                         [(int (/ sprite-x tilew))
                          (int (/ sprite-y tileh))]))
    (sprite c file)))

(defcomponent :s/image
  (schema/malli-form  [_ _schemas]
    m/image-schema)

  (db/edn->value [_ edn _db c]
    (edn->sprite c edn)))

(defcomponent :s/animation
  (schema/malli-form [_ _schemas]
    m/animation-schema)

  (db/edn->value [_ {:keys [frames frame-duration looping?]} _db c]
    (animation/create (map #(edn->sprite c %) frames)
                      :frame-duration frame-duration
                      :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defcomponent :s/one-to-one
  (schema/malli-form [[_ property-type] _schemas]
    (m/qualified-keyword-schema (type->id-namespace property-type)))
  (db/edn->value [_ property-id db c]
    (build c property-id)))

(defcomponent :s/one-to-many
  (schema/malli-form [[_ property-type] _schemas]
    (m/set-schema (m/qualified-keyword-schema (type->id-namespace property-type))))
  (db/edn->value [_ property-ids db c]
    (set (map #(build c %) property-ids))))

(defn- map-form [ks schemas]
  (m/map-schema ks (fn [k]
                     (schema/malli-form (schema/schema-of schemas k)
                                        schemas))))
; TODO schema/validate comes to this...
; but db-data is not yet existing?

(defmethod schema/malli-form :s/map [[_ ks] schemas]
  (map-form ks schemas))

(defmethod schema/malli-form :s/map-optional [[_ ks] schemas]
  (map-form (map (fn [k] [k {:optional true}]) ks)
            schemas))

(defn- namespaced-ks [schemas ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod schema/malli-form :s/components-ns [[_ ns-name-k] schemas]
  (schema/malli-form [:s/map-optional (namespaced-ks schemas ns-name-k)]
                     schemas))

(defn WASD-movement-vector [c]
  (let [r (when (key-pressed? c :d) [1  0])
        l (when (key-pressed? c :a) [-1 0])
        u (when (key-pressed? c :w) [0  1])
        d (when (key-pressed? c :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(def ^:private zoom-speed 0.025)

(defn check-camera-controls [{::keys [world-viewport] :as c}]
  (let [camera (:camera world-viewport)]
    (when (key-pressed? c :minus)  (cam/inc-zoom camera    zoom-speed))
    (when (key-pressed? c :equals) (cam/inc-zoom camera (- zoom-speed))))
  c)

(defn update-stage [context]
   (ui/act (stage context) context)
   context)
