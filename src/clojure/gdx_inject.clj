(in-ns 'clojure.core)

(require '[clojure.gdx :refer [black]]
         '[gdl.ui.group :as group]
         '[gdl.ui.actor :as actor])

(def children group/children)

(def toggle-visible! actor/toggle-visible!)
(def visible?        actor/visible?)
(def set-visible     actor/set-visible)
