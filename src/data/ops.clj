(ns data.ops
  (:refer-clojure :exclude [remove])
  (:require [utils.core :refer [update-kv safe-remove-one]]))

(def ^{:arglists '([ops to-add-ops   ])} add    (partial update-kv conj))
(def ^{:arglists '([ops to-remove-ops])} remove (partial update-kv safe-remove-one))

(comment
 (= (add {:+ [1 2 3]}
         {:* -0.5 :+ -1})
    {:+ [1 2 3 -1], :* [-0.5]})

 (= (remove {:+ [1 2 3] :* [-0.5]}
            {:+ 2 :* -0.5})
    {:+ [1 3], :* []})
 )
