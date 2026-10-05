// Draws a hall as a grid. Filled seats belong to this exam, hatched seats to another exam
// in the same time slot, and empty seats are the gaps left by the alternate-seat rule.
export default function SeatGrid({ rows, cols, seats = [], others = [], highlight = null }) {
  const mine = new Map(seats.map((s) => [`${s.seatRow}-${s.seatCol}`, s]))
  const other = new Map(others.map((o) => [`${o.seatRow}-${o.seatCol}`, o]))

  const cells = []
  for (let r = 1; r <= rows; r++) {
    cells.push(<div key={`r${r}`} className="seat-axis">{String.fromCharCode(64 + r)}</div>)
    for (let c = 1; c <= cols; c++) {
      const key = `${r}-${c}`
      const s = mine.get(key)
      const o = other.get(key)
      const isYou = highlight && highlight.row === r && highlight.col === c
      let cls = 'seat seat-empty'
      let text = ''
      let title = `Seat ${String.fromCharCode(64 + r)}-${c} (empty)`
      if (isYou) { cls = 'seat seat-you'; text = 'YOU'; title = 'Your seat' }
      else if (s) { cls = `seat seat-taken dept-${s.department}`; text = s.rollNo; title = `${s.name} (${s.rollNo}) seat ${s.seatLabel}` }
      else if (o) { cls = 'seat seat-other'; text = o.examCode; title = `Used by exam ${o.examCode}` }
      cells.push(<div key={key} className={cls} title={title}>{text}</div>)
    }
  }

  return (
    <div className="seat-scroll">
      <div className="seat-grid" style={{ gridTemplateColumns: `1.6rem repeat(${cols}, minmax(3.4rem, 1fr))` }}>
        <div />
        {Array.from({ length: cols }, (_, i) => <div key={i} className="seat-axis">{i + 1}</div>)}
        {cells}
      </div>
    </div>
  )
}
