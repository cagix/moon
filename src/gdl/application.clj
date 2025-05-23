; => space invaders <=
(ns gdl.application
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.string :as str]
            [gdl.c :as c]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.utils :as utils])
  (:import (clojure.lang IFn)
           (com.badlogic.gdx ApplicationAdapter
                             Gdx
                             Input$Keys
                             Input$Buttons)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)))

;; Publics

(def sound-path-format "sounds/%s.wav")

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

;;

(defn- k->code [key]
  (case key
    :minus  Input$Keys/MINUS
    :equals Input$Keys/EQUALS
    :space  Input$Keys/SPACE
    :p      Input$Keys/P
    :enter  Input$Keys/ENTER
    :escape Input$Keys/ESCAPE
    :i      Input$Keys/I
    :e      Input$Keys/E
    :d      Input$Keys/D
    :a      Input$Keys/A
    :w      Input$Keys/W
    :s      Input$Keys/S
    ))

(defn- button->code [button]
  (case button
    :left Input$Buttons/LEFT
    ))

(defn- asset-type->class ^Class [asset-type]
  (case asset-type
    :sound Sound
    :texture Texture))

(defn- asset-manager [assets]
  (let [manager (proxy [AssetManager IFn] []
                  (invoke [path]
                    (if (AssetManager/.contains this path)
                      (AssetManager/.get this ^String path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[file asset-type] assets]
      (.load manager ^String file (asset-type->class asset-type)))
    (.finishLoading manager)
    manager))

(defn- assets-of-type [^AssetManager assets asset-type]
  (filter #(= (.getAssetType assets %) (asset-type->class asset-type))
          (.getAssetNames assets)))

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- create-assets [{:keys [folder
                              asset-type-extensions]}]
  (asset-manager
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (recursively-search folder extensions))]
     [file asset-type])))

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

(defmulti ^:private draw! (fn [[k] _ctx]
                            k))

(defmethod draw! :draw/image [[_ {:keys [texture-region color] :as image} position]
                              {:keys [batch
                                      unit-scale]}]
  (graphics/draw-texture-region! batch
                                 texture-region
                                 position
                                 (unit-dimensions image unit-scale)
                                 0 ; rotation
                                 color))

(defmethod draw! :draw/rotated-centered [[_ {:keys [texture-region color] :as image} rotation [x y]]
                                         {:keys [batch
                                                 unit-scale]}]
  (let [[w h] (unit-dimensions image unit-scale)]
    (graphics/draw-texture-region! batch
                                   texture-region
                                   [(- (float x) (/ (float w) 2))
                                    (- (float y) (/ (float h) 2))]
                                   [w h]
                                   rotation
                                   color)))

(defmethod draw! :draw/centered [[_ image position] ctx]
  (draw! [:draw/rotated-centered image 0 position] ctx))

  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [default-font
                                     batch
                                     unit-scale]}]
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
                                {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [shape-drawer]}]
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
                                        {:keys [shape-drawer]
                                         :as ctx}]
  (sd/with-line-width shape-drawer width
    (fn []
      (c/handle-draws! ctx draws))))

; TODO
; !! -> use namespaced keywords <- !!

(defprotocol Disposable
  (dispose! [_]))

(defprotocol Viewports
  (update-viewports! [_]))

(defrecord Context [assets
                    batch
                    unit-scale
                    world-unit-scale
                    shape-drawer-texture
                    shape-drawer
                    cursors
                    default-font
                    world-viewport
                    ui-viewport
                    tiled-map-renderer
                    stage]
  Disposable
  (dispose! [_]
    (utils/dispose! assets)
    (utils/dispose! batch)
    (utils/dispose! shape-drawer-texture)
    (run! utils/dispose! (vals cursors))
    (utils/dispose! default-font)
    ; TODO vis-ui dispose
    )

  Viewports
  (update-viewports! [_]
    (graphics/update! ui-viewport)
    (graphics/update! world-viewport))

  c/Sounds
  (play-sound! [ctx sound-name]
    (->> sound-name
         (format sound-path-format)
         assets
         Sound/.play))

  (all-sounds [_]
    (assets-of-type assets :sound))

  c/Textures
  (texture [_ path]
    (assets path))

  (all-textures [_]
    (assets-of-type assets :texture))

  c/Graphics
  (delta-time [_]
    (graphics/delta-time))

  (clear-screen! [_]
    (graphics/clear-screen!))

  (handle-draws! [ctx draws]
    (doseq [component draws
            :when component]
      (draw! component ctx)))

  (set-camera-position! [_ position]
    (camera/set-position! (:camera world-viewport) position))

  (draw-on-world-viewport! [ctx fns]
    (graphics/draw-on-viewport! batch
                                world-viewport
                                (fn []
                                  (sd/with-line-width shape-drawer world-unit-scale
                                    (fn []
                                      (doseq [f fns]
                                        (f (assoc ctx :unit-scale world-unit-scale))))))))

  (draw-tiled-map! [_ tiled-map color-setter]
    (tiled/draw! (tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport)))

  (world-mouse-position [_]
    (graphics/mouse-position world-viewport))

  (ui-mouse-position [_]
    (graphics/mouse-position ui-viewport))

  (ui-viewport-width [_]
    (:width ui-viewport))

  (ui-viewport-height [_]
    (:height ui-viewport))

  (world-viewport-width [_]
    (:width world-viewport))

  (world-viewport-height [_]
    (:height world-viewport))

  (camera-position [_]
    (camera/position (:camera world-viewport)))

  (inc-zoom! [_ amount]
    (camera/inc-zoom! (:camera world-viewport) amount))

  (camera-frustum [_]
    (camera/frustum (:camera world-viewport)))

  (visible-tiles [_]
    (camera/visible-tiles (:camera world-viewport)))

  (camera-zoom [_]
    (camera/zoom (:camera world-viewport)))

  (pixels->world-units [_ pixels]
    (* pixels world-unit-scale))

  (sprite [this texture-path]
    (sprite* (graphics/texture-region (c/texture this texture-path))
             world-unit-scale))

  (sub-sprite [_ sprite [x y w h]]
    (sprite* (graphics/sub-region (:texture-region sprite) x y w h)
             world-unit-scale))

  (sprite-sheet [this texture-path tilew tileh]
    {:image (sprite* (graphics/texture-region (c/texture this texture-path))
                     world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [this {:keys [image tilew tileh]} [x y]]
    (c/sub-sprite this image [(* x tilew) (* y tileh) tilew tileh]))

  (set-cursor! [_ cursor-key]
    (graphics/set-cursor! (utils/safe-get cursors cursor-key)))

  c/Stage
  (draw-stage! [ctx]
    (reset! (.ctx stage) ctx)
    (ui/draw! stage)
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    ; => maybe context should be an immutable data structure with mutable fields?
    #_(reset! (.ctx stage) nil)
    nil)

  (update-stage! [ctx]
    (reset! (.ctx stage) ctx)
    (ui/act! stage)
    ; We cannot pass this
    ; because input events are handled outside ui/act! and in the Lwjgl3Input system:
    ;                         com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.<init>   Lwjgl3Application.java:  153
    ;                           com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.loop   Lwjgl3Application.java:  181
    ;                              com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window.update        Lwjgl3Window.java:  414
    ;                        com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input.update  DefaultLwjgl3Input.java:  190
    ;                                            com.badlogic.gdx.InputEventQueue.drain     InputEventQueue.java:   70
    ;                             gdl.ui.proxy$gdl.ui.CtxStage$ILookup$a65747ce.touchUp                         :
    ;                                     com.badlogic.gdx.scenes.scene2d.Stage.touchUp               Stage.java:  354
    ;                              com.badlogic.gdx.scenes.scene2d.InputListener.handle       InputListener.java:   71
    #_@(.ctx stage)
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    #_(reset! (.ctx stage) nil)
    nil)

  (get-actor [_ id]
    (id stage))

  (find-actor-by-name [_ name]
    (-> stage
        ui/root
        (ui/find-actor name)))

  (add-actor! [_ actor]
    (ui/add! stage actor))

  (mouseover-actor [this]
    (ui/hit stage (c/ui-mouse-position this)))

  (reset-actors! [_ actors]
    (ui/clear! stage)
    (run! #(ui/add! stage %) actors))

  c/Input
  (button-just-pressed? [_ button]
    (.isButtonJustPressed Gdx/input (button->code button)))

  (key-pressed? [_ key]
    (.isKeyPressed Gdx/input (k->code key)))

  (key-just-pressed? [_ key]
    (.isKeyJustPressed Gdx/input (k->code key))))

(defn- create-state! [config]
  (ui/load! (:ui config))
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))
        ui-viewport (graphics/ui-viewport (:ui-viewport config))
        stage (ui/stage (:java-object ui-viewport)
                        (:java-object batch))]
    (.setInputProcessor Gdx/input stage)
    (map->Context {:assets (create-assets (:assets config))
                   :batch batch
                   :unit-scale 1
                   :world-unit-scale world-unit-scale
                   :shape-drawer-texture shape-drawer-texture
                   :shape-drawer (graphics/shape-drawer batch (graphics/texture-region shape-drawer-texture 1 0 1 1))
                   :cursors (utils/mapvals
                             (fn [[file [hotspot-x hotspot-y]]]
                               (graphics/cursor (format (:cursor-path-format config) file)
                                                hotspot-x
                                                hotspot-y))
                             (:cursors config))
                   :default-font (graphics/truetype-font (:default-font config))
                   :world-viewport (graphics/world-viewport world-unit-scale (:world-viewport config))
                   :ui-viewport ui-viewport
                   :tiled-map-renderer (memoize (fn [tiled-map]
                                                  (tiled/renderer tiled-map
                                                                  world-unit-scale
                                                                  (:java-object batch))))
                   :stage stage})))

(def state (atom nil))

(defn start! [config create! render!]
  (lwjgl/application (:clojure.gdx.backends.lwjgl config)
                     (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state (create! (create-state! config)))
                         #_(ctx-schema/validate @state))

                       (dispose []
                         #_(ctx-schema/validate @state)
                         (dispose! @state)
                         #_(dispose! @state)
                         )

                       (render []
                         #_(ctx-schema/validate @state)
                         (swap! state render!)
                         #_(ctx-schema/validate @state))

                       (resize [_width _height]
                         #_(ctx-schema/validate @state)
                         (update-viewports! @state)))))
