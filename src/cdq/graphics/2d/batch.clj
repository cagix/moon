(ns cdq.graphics.2d.batch)

(defn set-projection-matrix [this projection]
  (.setProjectionMatrix this projection))

(defn begin [this]
  (.begin this))

(defn end [this]
  (.end this))

(defn set-color [this color]
  (.setColor this color))

(defn draw [this texture-region {:keys [x y origin-x origin-y width height scale-x scale-y rotation]}]
  (.draw this
         texture-region
         x
         y
         origin-x
         origin-y
         width
         height
         scale-x
         scale-y
         rotation))
