; https://github.com/libgdx/gdx-controllers/wiki
; https://github.com/MrStahlfelge/gdx-controllerutils
; https://javadoc.io/doc/com.badlogicgames.gdx/gdx-controllers/latest/index.html
; https://github.com/libgdx/gdx-controllers/blob/master/gdx-controllers-core/src/com/badlogic/gdx/controllers/ControllerMapping.java

#_(ns moon.controller-test
  (:require [forge.graphics :as app]
            [forge.graphics :refer [clear-screen]]
            [forge.graphics.text :as text]
            [forge.math.vector :as v]
            [forge.graphics :refer [batch draw-text shape-drawer]])
  (:import (com.badlogic.gdx.controllers Controllers)))

(declare ^:private controller)

#_(defn movement-vector []
  (let [v (v/normalise [   (.getAxis controller (.axisLeftX (.getMapping controller)))
                        (- (.getAxis controller (.axisLeftY (.getMapping controller))))])]
    (if (zero? (v/length v))
      nil
      v)))

(comment
 (.isConnected controller)

 ; this is not in javadoc
 ; latest version only github:
 ; https://github.com/libgdx/gdx-controllers/blob/29ddbc0639dd1defb839e79a224f77f8f2e4760b/gdx-controllers-core/src/com/badlogic/gdx/controllers/Controller.java#L72
 (.startVibration 500 0.5)
 )

#_(defn- draw-info []
  (if (and (bound? #'controller) controller)
    (do
     (draw-text {:x (- (/ gui-viewport-width 2) 300)
                 :y (- (/ gui-viewport-height 2) 200)
                 :text
                 (str
                  "(.isConnected my-controller): " (.isConnected controller) "\n"
                  "(.getAxis my-controller (.axisLeftX (.getMapping my-controller))): " (.getAxis controller (.axisLeftX (.getMapping controller))) "\n"
                  "(.getAxis my-controller (.axisLeftY (.getMapping my-controller))): " (.getAxis controller (.axisLeftY (.getMapping controller))) "\n"
                  "(moon.controls/movement-vector): " (movement-vector))})
     (let [start [(/ gui-viewport-width 2)
                    (/ gui-viewport-width 2)]]
       (when (movement-vector)
         (draw-line start
                    (v/add start (v/scale (movement-vector) 100))
                    :cyan))))
    (draw-text {:x (- (/ gui-viewport-width 2) 300)
                :y (- (/ gui-viewport-height 2) 200)
                :text (str "controller: " (pr-str controller) "\n press X to try to connect again." )}))
  #_(when (.getButton my-controller Xbox/A)
      (println "<a>")))

#_(defn -main []
  (app/start {:title "Controller Test"
              :width 800
              :height 800}
             (reify app/Listener
               (create [_]
                 ;(batch/init)
                 ;(shape-drawer/init)
                 (text/init nil)
                 (gui-view/init {:world-width 1440 :world-height 900})
                 (when-not (bound? #'controller)
                   (bind-root #'controller (first (Controllers/getControllers)))
                   (println "Controller bound - " controller)))

               (dispose [_]
                 ;(batch/dispose)
                 ;(shape-drawer/dispose)
                 (text/dispose))

               (render [_]
                 (clear-screen :black)
                 (view/render batch
                              shape-drawer
                              (deref (var gui-view/view))
                              draw-info)
                 (when (key-just-pressed? :x)
                   (when-not (or (bound? #'controller)
                                 (nil? controller))
                     (bind-root #'controller (first (Controllers/getControllers)))
                     (println "Controller bound - " controller))))

               (resize [_ dimensions]
                 (gui-view/resize dimensions)))))
