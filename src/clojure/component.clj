(ns clojure.component
  (:refer-clojure :exclude [apply]))

(defn dispatch [[k] & args]
  k)

(defmacro defsystem
  {:arglists '([name docstring?])}
  [name-sym & args]
  (let [docstring (if (string? (first args))
                    (first args))]
    `(defmulti ~name-sym
       ~(str "[[defsystem]]" (when docstring (str "\n\n" docstring)))
       dispatch)))

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render-below)
(defmethod render-below :default [_ entity])

(defsystem render-default)
(defmethod render-default :default [_ entity])

(defsystem render-above)
(defmethod render-above :default [_ entity])

(defsystem render-info)
(defmethod render-info :default [_ entity])

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem cursor)
(defmethod cursor :default [_])

(defsystem draw-gui-view)
(defmethod draw-gui-view :default [_])

(defsystem clicked-inventory-cell)
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill])

(defsystem render)
(defmethod render :default [_])

(defsystem info)
(defmethod info :default [_])

(defsystem apply)
(defsystem order)
(defsystem value-text)
