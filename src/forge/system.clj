(ns forge.system)

(declare assets
         batch
         shape-drawer
         default-font
         cursors
         cached-map-renderer
         world-unit-scale
         world-viewport-width
         world-viewport-height
         world-viewport
         gui-viewport-width
         gui-viewport-height
         gui-viewport
         screens
         current-screen-key
         schemas
         properties-file
         properties)

(defsystem create)

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])
