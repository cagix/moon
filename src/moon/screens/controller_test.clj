; https://github.com/libgdx/gdx-controllers/wiki
; https://github.com/MrStahlfelge/gdx-controllerutils
; https://javadoc.io/doc/com.badlogicgames.gdx/gdx-controllers/latest/index.html
; https://github.com/libgdx/gdx-controllers/blob/master/gdx-controllers-core/src/com/badlogic/gdx/controllers/ControllerMapping.java

(ns moon.screens.controller-test
  (:require [gdl.app :as app]
            [gdl.graphics :refer [clear-screen]]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.view :as view]
            [gdl.graphics.gui-view :as gui-view]
            [gdl.graphics.text :as text]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.input :refer [key-just-pressed?]]
            [gdl.math.vector :as v])
  (:import (com.badlogic.gdx.controllers Controllers)))

(declare ^:private controller)

(defn movement-vector []
  (let [v (v/normalise [   (.getAxis controller (.axisLeftX (.getMapping controller)))
                        (- (.getAxis controller (.axisLeftY (.getMapping controller))))])]
    (if (zero? (v/length v))
      nil
      v)))

(comment
 (.isConnected controller)
 (.startVibration 500 0.5)
 )

(defn- draw-info []
  (if (and (bound? #'controller) controller)
    (do
     (text/draw {:x (- (/ (gui-view/width) 2) 300)
                 :y (- (/ (gui-view/height) 2) 200)
                 :text
                 (str
                  "(.isConnected my-controller): " (.isConnected controller) "\n"
                  "(.getAxis my-controller (.axisLeftX (.getMapping my-controller))): " (.getAxis controller (.axisLeftX (.getMapping controller))) "\n"
                  "(.getAxis my-controller (.axisLeftY (.getMapping my-controller))): " (.getAxis controller (.axisLeftY (.getMapping controller))) "\n"
                  "(moon.controls/movement-vector): " (movement-vector))})
     (let [start [(/ (gui-view/width) 2)
                    (/ (gui-view/width) 2)]]
       (when (movement-vector)
         (shape-drawer/line start
                            (v/add start (v/scale (movement-vector) 100))
                            :cyan))))
    (text/draw {:x (- (/ (gui-view/width) 2) 300)
                :y (- (/ (gui-view/height) 2) 200)
                :text (str "controller: " (pr-str controller) "\n press X to try to connect again." )}))
  #_(when (.getButton my-controller Xbox/A)
      (println "<a>")))

(defn -main []
  (app/start {:title "Controller Test"
              :width 800
              :height 800}
             (reify app/Listener
               (create [_]
                 (batch/init)
                 (shape-drawer/init)
                 (text/init nil)
                 (gui-view/init {:world-width 1440 :world-height 900})
                 (when-not (bound? #'controller)
                   (bind-root #'controller (first (Controllers/getControllers)))
                   (println "Controller bound - " controller)))

               (dispose [_]
                 (batch/dispose)
                 (shape-drawer/dispose)
                 (text/dispose))

               (render [_]
                 (clear-screen :black)
                 (view/render (deref (var gui-view/view))
                              draw-info)
                 (when (key-just-pressed? :x)
                   (when-not (or (bound? #'controller)
                                 (nil? controller))
                     (bind-root #'controller (first (Controllers/getControllers)))
                     (println "Controller bound - " controller))))

               (resize [_ dimensions]
                 (gui-view/resize dimensions)))))
