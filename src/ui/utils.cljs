(ns ui.utils)

(defn string-hash [s]
  (loop [i 0 h 0]
    (if (< i (.-length s))
      (let [c (.charCodeAt s i)
            h (+ (- (bit-shift-left h 5) h) c)]
        (recur (inc i) (bit-and h h)))
      h)))
