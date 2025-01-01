(ns clojure.component)

(defmacro defsystem
  {:arglists '([name docstring?])}
  [name-sym & args]
  (let [docstring (if (string? (first args))
                    (first args))]
    `(defmulti ~name-sym
       ~(str "[[defsystem]]" (when docstring (str "\n\n" docstring)))
       (fn [[k#] & args#]
         k#))))

(def overwrite-warnings? false)

(defmacro defcomponent [k & sys-impls]
  `(do
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)]]
        `(do
          (when (and overwrite-warnings?
                     (get (methods @~sys-var) ~k))
            (println "warning: overwriting defmethod" ~k "on" ~sys-var))
          (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
            ~@fn-body)))
    ~k))

(defsystem ->v)
(defmethod ->v :default [[_ v] _context]
  v)

(defsystem create!)
(defmethod create! :default [_ eid c])

(defsystem destroy)
(defmethod destroy :default [_ eid c])

(defsystem tick)
(defmethod tick :default [_ eid c])

(defsystem render-below)
(defmethod render-below :default [_ entity c])

(defsystem render-default)
(defmethod render-default :default [_ entity c])

(defsystem render-above)
(defmethod render-above :default [_ entity c])

(defsystem render-info)
(defmethod render-info :default [_ entity c])

(defsystem enter)
(defmethod enter :default [_ c])

(defsystem exit)
(defmethod exit :default [_ c])

(defsystem cursor)
(defmethod cursor :default [_])

(defsystem clicked-inventory-cell)
(defmethod clicked-inventory-cell :default [_ cell c])

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill c])

(defsystem draw-gui-view)
(defmethod draw-gui-view :default [_ c])

(defsystem manual-tick)
(defmethod manual-tick :default [_ c])

(defsystem pause-game?)
(defmethod pause-game? :default [_])
