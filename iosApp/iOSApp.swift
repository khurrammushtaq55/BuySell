//
//  iOSApp.swift
//  BuySell360 iOS App Entry Point
//

import SwiftUI
import shared

@main
struct BuySell360App: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    @StateObject private var viewModel = DashboardViewModelWrapper()

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Image(systemName: "cart.fill")
                    .resizable()
                    .frame(width: 80, height: 80)
                    .foregroundColor(.green)

                Text("BuySell360 iOS Pro")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Net Profit (Shared KMP Logic):")
                    .font(.subheadline)
                    .foregroundColor(.gray)

                Text("\(viewModel.netProfitAmount) Paisa")
                    .font(.title)
                    .fontWeight(.semibold)
                    .foregroundColor(.green)

                Button(action: {
                    viewModel.refreshMetrics()
                }) {
                    Text("Recalculate Profit (KMP)")
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
            }
            .navigationTitle("BuySell360")
        }
    }
}

class DashboardViewModelWrapper: ObservableObject {
    @Published var netProfitAmount: Int64 = 0
    private let sharedVm = SharedOwnerDashboardViewModel()

    init() {
        refreshMetrics()
    }

    func refreshMetrics() {
        sharedVm.updateMetrics(
            grossRevenueRs: 15000000,
            returnsRefundsRs: 500000,
            grossCogsRs: 10000000,
            returnedCogsRs: 300000,
            totalExpensesRs: 800000
        )
        let summary = sharedVm.uiState.value.financialSummary
        self.netProfitAmount = summary.netProfitRs
    }
}
