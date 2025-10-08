(ns clojure.gdx.shape-drawer)

(defmacro with-line-width [shape-drawer width & exprs]
  `(let [old-line-width# (.getDefaultLineWidth ~shape-drawer)]
     (.setDefaultLineWidth ~shape-drawer (* ~width old-line-width#))
     ~@exprs
     (.setDefaultLineWidth ~shape-drawer old-line-width#)))
