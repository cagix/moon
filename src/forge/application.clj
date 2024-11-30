(ns forge.application
  (:refer-clojure :exclude [do])
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils :refer [mac?]]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defprotocol Listener
  (create [_])
  (dispose [_])
  (render [_])
  (resize [_ w h]))

(defn start [{:keys [dock-icon title fps width height]} listener]
  (awt/set-dock-icon (io/resource dock-icon))
  (when mac?
    (lwjgl3/configure-glfw-for-mac))
  (lwjgl3/application (proxy [ApplicationAdapter] []
                        (create []
                          (create listener))

                        (dispose []
                          (dispose listener))

                        (render []
                          (render listener))

                        (resize [w h]
                          (resize listener w h)))
                      (lwjgl3/config {:title title
                                      :fps fps
                                      :width width
                                      :height height})))

(def exit gdx/exit)

(defmacro do [& exprs]
  `(gdx/post-runnable (fn [] ~@exprs)))
