(ns gdl.context.ui
  (:require [clojure.gdx.vis-ui :as vis-ui])
  (:import (com.kotcrab.vis.ui.widget Tooltip)))

(defn create [[_ skin-scale] _c]
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

(defn dispose [_]
  (vis-ui/dispose))
