(ns forge.app.vis-ui
  (:require [clojure.vis-ui :as vis]))

(defn create [[_ skin-scale]]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (vis/loaded?)
    (vis/dispose))
  (vis/load skin-scale)
  (-> (vis/skin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  (vis/configure-tooltips {:default-appear-delay-time 0}))

(defn dispose [_]
  (vis/dispose))
