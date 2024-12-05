(ns clojure.vis-ui
  (:refer-clojure :exclude [load])
  (:import (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)))

(defn loaded? [] (VisUI/isLoaded))
(defn dispose [] (VisUI/dispose))
(defn skin    [] (VisUI/getSkin))

(defn load [skin-scale]
  (VisUI/load (case skin-scale
                :skin-scale/x1 VisUI$SkinScale/X1
                :skin-scale/x2 VisUI$SkinScale/X2)))

(defn configure-tooltips [{:keys [default-appear-delay-time]}]
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float default-appear-delay-time)))
