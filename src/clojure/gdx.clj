(in-ns 'clojure.core)

(require '[clojure.gdx.input :as input]
         '[gdl.ui.group :as group]
         '[gdl.ui.actor :as actor])

(def key-just-pressed?    input/key-just-pressed?)
(def key-pressed?         input/key-pressed?)
(def button-just-pressed? input/button-just-pressed?)
(def set-input-processor  input/set-processor)

(def children group/children)

(def toggle-visible! actor/toggle-visible!)
(def visible?        actor/visible?)
(def set-visible     actor/set-visible)
