(ns gdl.application.desktop
  (:require [clojure.gdx.interop :as interop]
            [clojure.java.io :as io]
            [gdl.app]
            [gdl.audio]
            [gdl.file]
            [gdl.fs]
            [gdl.graphics]
            [gdl.input]
            [gdl.ui]
            [gdl.utils.disposable])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Pixmap
                                      Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group
                                            Touchable)
           (com.badlogic.gdx.utils Disposable
                                   SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(extend-type TextureRegion
  gdl.graphics.g2d.texture-region/TextureRegion
  (dimensions [texture-region]
    [(.getRegionWidth  texture-region)
     (.getRegionHeight texture-region)])
  (region [texture-region x y w h]
    (TextureRegion. texture-region
                    (int x)
                    (int y)
                    (int w)
                    (int h))))

(extend-type Texture
  gdl.graphics.texture/Texture
  (region
    ([texture]
     (TextureRegion. texture))
    ([texture x y w h]
     (TextureRegion. texture
                     (int x)
                     (int y)
                     (int w)
                     (int h)))))

(extend-type Group
  gdl.ui/PGroup
  (find-actor [group name]
    (.findActor group name))
  (clear-children! [group]
    (.clearChildren group))
  (children [group]
    (.getChildren group)))

(extend-type Actor
  gdl.ui/PActor
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
    (.setTouchable actor (case touchable
                           :disabled Touchable/disabled)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor)))

(extend-type Sound
  gdl.audio/Sound
  (play! [this]
    (.play this)))

(extend-type Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(extend-type FileHandle
  gdl.file/File
  (list [this]
    (.list this))
  (directory? [this]
    (.isDirectory this))
  (extension [this]
    (.extension this))
  (path [this]
    (.path this)))

(defrecord Context [app
                    audio
                    files
                    graphics
                    input]
  gdl.app/Application
  (post-runnable! [_ runnable]
    (.postRunnable app runnable))

  gdl.fs/FileSystem
  (internal [_ path]
    (.internal files path))

  gdl.audio/Sounds
  (sound [_ path]
    (.newSound audio (.internal files path)))

  gdl.graphics/Cursors
  (cursor [_ path [hotspot-x hotspot-y]]
    (let [pixmap (Pixmap. (.internal files path))
          cursor (.newCursor graphics pixmap hotspot-x hotspot-y)]
      (.dispose pixmap)
      cursor))

  gdl.input/Input
  (set-input-processor! [_ input-processor]
    (.setInputProcessor input input-processor))

  (button-just-pressed? [_ button]
    (.isButtonJustPressed input (interop/k->input-button button)))

  (key-pressed? [_ key]
    (.isKeyPressed input (interop/k->input-key key)))

  (key-just-pressed? [_ key]
    (.isKeyJustPressed input (interop/k->input-key key)))

  (mouse-position [_]
    [(.getX input)
     (.getY input)]))

(defn- create-context []
  (map->Context {:app      Gdx/app
                 :audio    Gdx/audio
                 :files    Gdx/files
                 :graphics Gdx/graphics
                 :input    Gdx/input}))

(defn- apply-mac-os-settings!
  [{:keys [glfw-async?
           dock-icon]}]
  (when glfw-async?
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (when dock-icon
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon)))))

(defn start! [{:keys [mac-os-settings
                      create
                      dispose
                      render
                      resize
                      title
                      windowed-mode
                      foreground-fps]}]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (apply-mac-os-settings! mac-os-settings))
  (Lwjgl3Application. (proxy [ApplicationListener] []
                        (create []
                          (let [[f params] create]
                            (f {:ctx/gdl (create-context)} params)))
                        (dispose []
                          (dispose))
                        (render  []
                          (let [[f params] render]
                            (f params)))
                        (resize [width height]
                          (resize width height))
                        (pause [])
                        (resume []))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width  windowed-mode)
                                          (:height windowed-mode))
                        (.setForegroundFPS foreground-fps))))
