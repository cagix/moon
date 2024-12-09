(ns anvil.app
  (:require [clojure.awt :as awt]
            [clojure.component :refer [defsystem] :as component]
            [clojure.gdx.app :as app]
            [clojure.gdx.asset-manager :as manager]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl :as lwjgl]
            [clojure.string :as str]
            [clojure.utils :refer [bind-root defmethods]]))

(defsystem create)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defmethods ::asset-manager
  (create [[_ folder]]
    (def asset-manager (manager/load-all
                        (for [[asset-type exts] [[:sound   #{"wav"}]
                                                 [:texture #{"png" "bmp"}]]
                              file (map #(str/replace-first % folder "")
                                        (files/recursively-search folder exts))]
                          [file asset-type]))))

  (dispose [_]
    (disposable/dispose asset-manager)))

(defmethods ::sprite-batch
  (create [_]
    (def batch (g/sprite-batch)))

  (dispose [_]
    (disposable/dispose batch)))

(let [pixel-texture (atom nil)]
  (defmethods ::shape-drawer
    (create [_]
      (reset! pixel-texture (let [pixmap (doto (g/pixmap 1 1)
                                           (.setColor color/white)
                                           (.drawPixel 0 0))
                                  texture (g/texture pixmap)]
                              (disposable/dispose pixmap)
                              texture))
      (def sd (sd/create batch (g/texture-region @pixel-texture 1 0 1 1))))

    (dispose [_]
      (disposable/dispose @pixel-texture))))

(defmethods ::default-font
  (create [[_ font]]
    (def default-font (freetype/generate-font font)))

  (dispose [_]
    (disposable/dispose default-font)))

(defn start [{:keys [dock-icon components lwjgl3]}]
  (awt/set-dock-icon dock-icon)
  (when shared-library-loader/mac?
    (lwjgl/configure-glfw-for-mac))
  (lwjgl3/app (reify lwjgl3/Listener
                (create  [_]     (run! create          components))
                (dispose [_]     (run! dispose         components))
                (render  [_]     (run! render          components))
                (resize  [_ w h] (run! #(resize % w h) components)))
              (lwjgl3/config lwjgl3)))

(def exit app/exit)

(defmacro post-runnable [& exprs]
  `(app/post-runnable (fn [] ~@exprs)))

(declare screens
         current-screen-key)

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (component/exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root current-screen-key new-k)
    (component/enter screen)))
