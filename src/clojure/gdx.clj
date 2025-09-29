(ns clojure.gdx
  (:require clojure.audio
            clojure.audio.sound
            clojure.disposable
            clojure.files
            clojure.files.file-handle
            clojure.graphics
            clojure.graphics.batch
            clojure.graphics.bitmap-font
            clojure.graphics.orthographic-camera
            clojure.graphics.pixmap
            clojure.graphics.shape-drawer
            clojure.graphics.texture
            clojure.graphics.texture-region
            clojure.graphics.viewport
            clojure.input

            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.ctx :as ctx]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.cell :as cell]
            [clojure.scene2d.ui.table :as table]
            [clojure.scene2d.ui.widget-group :as widget-group]

            [clojure.core-ext :refer [clamp]]
            [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3.application :as lwjgl3-application]
            [com.badlogic.gdx.graphics.color :as color]
            [com.badlogic.gdx.graphics.orthographic-camera :as orthographic-camera]
            [com.badlogic.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [com.badlogic.gdx.input.buttons :as input-buttons]
            [com.badlogic.gdx.input.keys    :as input-keys]
            [com.badlogic.gdx.math.vector2 :as vector2]
            [com.badlogic.gdx.scenes.scene2d.touchable :as touchable]
            [com.badlogic.gdx.utils.align :as align]
            [com.badlogic.gdx.utils.viewport :as viewport]
            [com.badlogic.gdx.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx
                             Graphics
                             Input)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch
                                          BitmapFont
                                          TextureRegion
                                          SpriteBatch)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Table)
           (com.badlogic.gdx.utils Disposable)
           (clojure.scene2d Stage)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn application [config]
  (lwjgl3-application/set-glfw-async!)
  (lwjgl3-application/start! (reify ApplicationListener
                               (create [_]
                                 ((:create config) {:ctx/audio    (gdx/audio)
                                                    :ctx/files    (gdx/files)
                                                    :ctx/graphics (gdx/graphics)
                                                    :ctx/input    (gdx/input)}))
                               (dispose [_]
                                 ((:dispose config)))
                               (render [_]
                                 ((:render config)))
                               (resize [_ width height]
                                 ((:resize config) width height))
                               (pause [_])
                               (resume [_]))
                             config))

(defn post-runnable! [f]
  (.postRunnable Gdx/app f))

(def orthographic-camera orthographic-camera/create)

(defn shape-drawer [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn stage [viewport batch]
  (Stage. viewport batch))

;;;;; extend-types

(extend-type Audio
  clojure.audio/Audio
  (new-sound [this file-handle]
    (.newSound this file-handle)))

(extend-type Sound
  clojure.audio.sound/Sound
  (play! [this]
    (.play this)))

(extend-type Files
  clojure.files/Files
  (internal [this path]
    (.internal this path)))

(extend-type FileHandle
  clojure.files.file-handle/FileHandle
  (list [this]
    (.list this))

  (directory? [this]
    (.isDirectory this))

  (extension [this]
    (.extension this))

  (path [this]
    (.path this)))

(extend-type Disposable
  clojure.disposable/Disposable
  (dispose! [this]
    (.dispose this)))

(extend-type Batch
  clojure.graphics.batch/Batch
  (draw! [this texture-region x y [w h] rotation]
    (.draw this
           texture-region
           x
           y
           (/ (float w) 2) ; origin-x
           (/ (float h) 2) ; origin-y
           w
           h
           1 ; scale-x
           1 ; scale-y
           rotation))

  (set-color! [this [r g b a]]
    (.setColor this r g b a))

  (set-projection-matrix! [this matrix]
    (.setProjectionMatrix this matrix))

  (begin! [this]
    (.begin this))

  (end! [this]
    (.end this)))

(extend-type BitmapFont
  clojure.graphics.bitmap-font/BitmapFont
  (draw! [font
          batch
          {:keys [scale text x y up? h-align target-width wrap?]}]
    {:pre [(or (nil? h-align)
               (contains? align/k->value h-align))]}
    (let [old-scale (.scaleX (.getData font))]
      (.setScale (.getData font) (float (* old-scale scale)))
      (.draw font
             batch
             text
             (float x)
             (float (+ y (if up? (bitmap-font/text-height font text) 0)))
             (float target-width)
             (get align/k->value (or h-align :center))
             wrap?)
      (.setScale (.getData font) (float old-scale)))))

(extend-type TextureRegion
  clojure.graphics.texture-region/TextureRegion
  (dimensions [texture-region]
    [(.getRegionWidth  texture-region)
     (.getRegionHeight texture-region)]))

(extend-type Graphics
  clojure.graphics/Graphics
  (delta-time [this]
    (.getDeltaTime this))
  (frames-per-second [this]
    (.getFramesPerSecond this))
  (set-cursor! [this cursor]
    (.setCursor this cursor))
  (cursor [this pixmap hotspot-x hotspot-y]
    (.newCursor this pixmap hotspot-x hotspot-y))
  (clear!
    ([this [r g b a]]
     (clojure.graphics/clear! this r g b a))
    ([this r g b a]
     (let [clear-depth? false
           apply-antialiasing? false
           gl20 (.getGL20 this)]
       (GL20/.glClearColor gl20 r g b a)
       (let [mask (cond-> GL20/GL_COLOR_BUFFER_BIT
                    clear-depth? (bit-or GL20/GL_DEPTH_BUFFER_BIT)
                    (and apply-antialiasing? (.coverageSampling (.getBufferFormat this)))
                    (bit-or GL20/GL_COVERAGE_BUFFER_BIT_NV))]
         (GL20/.glClear gl20 mask)))))
  (texture [_ file-handle]
    (Texture. ^FileHandle file-handle))
  (pixmap
    ([_ ^FileHandle file-handle]
     (Pixmap. file-handle))
    ([_ width height pixmap-format]
     (Pixmap. (int width)
              (int height)
              (case pixmap-format
                :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888))))

  (fit-viewport [_ width height camera]
    (fit-viewport/create width height camera))

  (sprite-batch [_]
    (SpriteBatch.)))

(extend-type OrthographicCamera
  clojure.graphics.orthographic-camera/OrthographicCamera
  (set-position! [this [x y]]
    (set! (.x (.position this)) (float x))
    (set! (.y (.position this)) (float y))
    (.update this))

  (set-zoom! [this amount]
    (set! (.zoom this) amount)
    (.update this)))

(extend-type Pixmap
  clojure.graphics.pixmap/Pixmap
  (set-color! [pixmap [r g b a]]
    (.setColor pixmap r g b a))

  (draw-pixel! [pixmap x y]
    (.drawPixel pixmap x y))

  (texture [pixmap]
    (Texture. pixmap)))

(extend-type com.badlogic.gdx.utils.viewport.FitViewport
  clojure.graphics.viewport/Viewport
  (update! [this width height {:keys [center?]}]
    (viewport/update! this width height :center? center?))

  (unproject [this [x y]]
    (viewport/unproject this
                        (clamp x
                               (:viewport/left-gutter-width this)
                               (:viewport/right-gutter-x    this))
                        (clamp y
                               (:viewport/top-gutter-height this)
                               (:viewport/top-gutter-y      this)))))

(extend-type ShapeDrawer
  clojure.graphics.shape-drawer/ShapeDrawer
  (set-color! [this color]
    (.setColor this (color/float-bits color)))

  (with-line-width [this width draw-fn]
    (let [old-line-width (.getDefaultLineWidth this)]
      (.setDefaultLineWidth this (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth this (float old-line-width))))

  (arc! [this center-x center-y radius start-radians radians]
    (.arc this
          (float center-x)
          (float center-y)
          (float radius)
          (float start-radians)
          (float radians)))

  (circle! [this x y radius]
    (.circle this
             (float x)
             (float y)
             (float radius)))

  (ellipse! [this x y radius-x radius-y]
    (.ellipse this
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (filled-circle! [this x y radius]
    (.filledCircle this
                   (float x)
                   (float y)
                   (float radius)))

  (filled-ellipse! [this x y radius-x radius-y]
    (.filledEllipse this
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (filled-rectangle! [this x y w h]
    (.filledRectangle this
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (line! [this sx sy ex ey]
    (.line this
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (rectangle! [this x y w h]
    (.rectangle this
                (float x)
                (float y)
                (float w)
                (float h)))

  (sector! [this center-x center-y radius start-radians radians]
    (.sector this
             (float center-x)
             (float center-y)
             (float radius)
             (float start-radians)
             (float radians))))

(extend-type Texture
  clojure.graphics.texture/Texture
  (region
    ([texture]
     (TextureRegion. texture))
    ([texture [x y w h]]
     (TextureRegion. texture
                     (int x)
                     (int y)
                     (int w)
                     (int h)))
    ([texture x y w h]
     (TextureRegion. texture
                     (int x)
                     (int y)
                     (int w)
                     (int h)))))

(extend-type Input
  clojure.input/Input
  (button-just-pressed? [this button]
    {:pre [(contains? input-buttons/k->value button)]}
    (.isButtonJustPressed this (input-buttons/k->value button)))

  (key-pressed? [this key]
    (assert (contains? input-keys/keyword->value key)
            (str "(pr-str key): "(pr-str key)))
    (.isKeyPressed this (input-keys/keyword->value key)))

  (key-just-pressed? [this key]
    {:pre [(contains? input-keys/keyword->value key)]}
    (.isKeyJustPressed this (input-keys/keyword->value key)))

  (set-processor! [this input-processor]
    (.setInputProcessor this input-processor))

  (mouse-position [this]
    [(.getX this)
     (.getY this)]))

(extend-type Stage
  clojure.scene2d.stage/Stage
  (set-ctx! [stage ctx]
    (set! (.ctx stage) ctx))

  (get-ctx [stage]
    (.ctx stage))

  (act! [stage]
    (.act stage))

  (draw! [stage]
    (.draw stage))

  (add! [stage actor]
    (.addActor stage actor))

  (clear! [stage]
    (.clear stage))

  (root [stage]
    (.getRoot stage))

  (hit [stage [x y]]
    (.hit stage x y true))

  (viewport [stage]
    (.getViewport stage)))

(defn- get-ctx [actor]
  (when-let [stage (actor/get-stage actor)]
    (stage/get-ctx stage)))

(def ^:private opts-fn-map
  {:actor/name (fn [a v] (actor/set-name! a v))
   :actor/user-object (fn [a v] (actor/set-user-object! a v))
   :actor/visible?  (fn [a v] (actor/set-visible! a v))
   :actor/touchable (fn [a v] (actor/set-touchable! a v))
   :actor/listener (fn [a v] (actor/add-listener! a v))
   :actor/position (fn [actor [x y]]
                     (actor/set-position! actor x y))
   :actor/center-position (fn [actor [x y]]
                            (actor/set-position! actor
                                                 (- x (/ (actor/get-width  actor) 2))
                                                 (- y (/ (actor/get-height actor) 2))))})

(extend-type Actor
  clojure.scene2d.actor/Actor
  (get-stage [actor]
    (.getStage actor))

  (get-x [actor]
    (.getX actor))

  (get-y [actor]
    (.getY actor))

  (get-name [actor]
    (.getName actor))

  (user-object [actor]
    (.getUserObject actor))

  (set-user-object! [actor object]
    (.setUserObject actor object))

  (visible? [actor]
    (.isVisible actor))

  (set-visible! [actor visible?]
    (.setVisible actor visible?))

  (set-touchable! [actor touchable]
    (.setTouchable actor (touchable/k->value touchable)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor))

  (stage->local-coordinates [actor position]
    (-> actor
        (.stageToLocalCoordinates (vector2/->java position))
        vector2/->clj))

  (hit [actor [x y]]
    (.hit actor x y true))

  (set-name! [actor name]
    (.setName actor name))

  (set-position! [actor x y]
    (.setPosition actor x y))

  (get-width [actor]
    (.getWidth actor))

  (get-height [actor]
    (.getHeight actor))

  (add-listener! [actor listener]
    (.addListener actor listener))

  (set-opts! [actor opts]
    (doseq [[k v] opts
            :let [f (get opts-fn-map k)]
            :when f]
      (f actor v))
    actor)

  (act [actor delta f]
    (when-let [ctx (get-ctx actor)]
      (f actor delta ctx)))

  (draw [actor f]
    (when-let [ctx (get-ctx actor)]
      (ctx/draw! ctx (f actor ctx)))))

(defn- create-actor*
  [{:keys [actor/act
           actor/draw]
    :as opts}]
  (doto (proxy [Actor] []
          (act [delta]
            (act this delta)
            (proxy-super act delta))
          (draw [batch parent-alpha]
            (draw this batch parent-alpha)))
    (actor/set-opts! opts)))

(defmethod scene2d/build :actor.type/actor
  [opts]
  (create-actor*
   (assoc opts
          :actor/act (fn [actor delta]
                       (when (:act opts)
                         (actor/act actor delta (:act opts))))
          :actor/draw (fn [actor _batch _parent-alpha]
                        (when (:draw opts)
                          (actor/draw actor (:draw opts)))))))

(defn- build? [actor-or-decl]
  (try (cond
        (instance? Actor actor-or-decl)
        actor-or-decl
        (nil? actor-or-decl)
        nil
        :else
        (scene2d/build actor-or-decl))
       (catch Throwable t
         (throw (ex-info ""
                         {:actor-or-decl actor-or-decl}
                         t)))))

(extend-type Table
  clojure.scene2d.ui.table/Table
  (add! [table actor-or-decl]
    (.add table ^Actor (build? actor-or-decl)))

  (cells [table]
    (.getCells table))

  (add-rows! [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (cond
         (map? props-or-actor) (-> (table/add! table (:actor props-or-actor))
                                   (cell/set-opts! (dissoc props-or-actor :actor)))
         :else (table/add! table props-or-actor)))
      (.row table))
    table)

  (set-opts! [table {:keys [rows cell-defaults] :as opts}]
    (cell/set-opts! (.defaults table) cell-defaults)
    (doto table
      (table/add-rows! rows)
      (widget-group/set-opts! opts))))

(extend-type Group
  clojure.scene2d.group/Group
  (add! [group actor]
    (.addActor group actor))

  (find-actor [group name]
    (.findActor group name))

  (clear-children! [group]
    (.clearChildren group))

  (children [group]
    (.getChildren group))

  (set-opts! [group opts]
    (run! (fn [actor-or-decl]
            (group/add! group (if (instance? Actor actor-or-decl)
                          actor-or-decl
                          (scene2d/build actor-or-decl))))
          (:group/actors opts))
    (actor/set-opts! group opts)))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (Group.)
    (group/set-opts! opts)))
